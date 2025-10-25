/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.api;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.common.ErrorCodeEnum;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentTransformerOptions;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeStoreOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeAPISpec;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author nuocheng.lxm
 * @author yuluo
 * @author YunKui Lu
 * @since 1.0.0-M2
 */
public class DashScopeApi {

	private static final Predicate<String> SSE_DONE_PREDICATE = "[DONE]"::equals;

	// Store config fields for mutate/copy
	private final String baseUrl;

	private final ApiKey apiKey;

	private final String completionsPath;

	private final String embeddingsPath;

	private final MultiValueMap<String, String> headers;

	/**
	 * Default chat model
	 */
	public static final String DEFAULT_CHAT_MODEL = DashScopeModel.ChatModel.QWEN_PLUS.getValue();

	public static final String DEFAULT_EMBEDDING_MODEL = DashScopeModel.EmbeddingModel.EMBEDDING_V2.getValue();

	public static final String DEFAULT_EMBEDDING_TEXT_TYPE = DashScopeModel.EmbeddingTextType.DOCUMENT.getValue();

	private final RestClient restClient;

	private final WebClient webClient;

	private final ResponseErrorHandler responseErrorHandler;

	/**
	 * Returns a builder pre-populated with the current configuration for mutation.
	 */
	public Builder mutate() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Create a new chat completion api.
	 * @param baseUrl api base URL.
	 * @param apiKey OpenAI apiKey.
	 * @param header the http headers to use.
	 * @param completionsPath the path to the chat completions endpoint.
	 * @param embeddingsPath the path to the embeddings endpoint.
	 * @param workSpaceId the workspace ID to use.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	// @formatter:off
	public DashScopeApi(
			String baseUrl,
			ApiKey apiKey,
			MultiValueMap<String, String> header,
			String completionsPath,
			String embeddingsPath,
			// Add request header.
			String workSpaceId,
			RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler
	) {

		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
		this.headers = header;
		this.completionsPath = completionsPath;
		this.embeddingsPath = embeddingsPath;
		this.responseErrorHandler = responseErrorHandler;

		// For DashScope API, the workspace ID is passed in the headers.
		if (StringUtils.hasText(workSpaceId)) {
			this.headers.add(DashScopeApiConstants.HEADER_WORK_SPACE_ID, workSpaceId);
		}

		// Check API Key in headers.
		Consumer<HttpHeaders> finalHeaders = h -> {
			if (!(apiKey instanceof NoopApiKey)) {
				h.setBearerAuth(apiKey.getValue());
			}

			h.setContentType(MediaType.APPLICATION_JSON);
			h.addAll(headers);
		};

		this.restClient = restClientBuilder.clone()
				.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders)
				.defaultStatusHandler(responseErrorHandler)
				.build();

		this.webClient = webClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(finalHeaders)
				.build();
	}
	// @formatter:on

	/*******************************************
	 * Embedding
	 ******************************************/

    public ResponseEntity<DashScopeAPISpec.EmbeddingList> embeddings(DashScopeAPISpec.EmbeddingRequest embeddingRequest) {

        Assert.notNull(embeddingRequest, "The request body can not be null.");
        Assert.notNull(embeddingRequest.input(), "The input can not be null.");
        Assert.isTrue(!CollectionUtils.isEmpty(embeddingRequest.input().texts()), "The input texts can not be empty.");
        Assert.isTrue(embeddingRequest.input().texts().size() <= 25, "The input texts limit 25.");

        return this.restClient.post()
                .uri(this.embeddingsPath)
                .headers(this::addDefaultHeadersIfMissing)
                .body(embeddingRequest)
                .retrieve()
                .toEntity(DashScopeAPISpec.EmbeddingList.class);
    }

	public String upload(File file, DashScopeAPISpec.UploadRequest request) {
		// apply to upload
		ResponseEntity<DashScopeAPISpec.UploadLeaseResponse> responseEntity = uploadLease(request);
		var uploadLeaseResponse = responseEntity.getBody();
		if (uploadLeaseResponse == null) {
			throw new DashScopeException(ErrorCodeEnum.READER_APPLY_LEASE_ERROR);
		}
		if (!"SUCCESS".equalsIgnoreCase(uploadLeaseResponse.code())) {
			throw new DashScopeException("ApplyLease Failed,code:%s,message:%s".formatted(uploadLeaseResponse.code(),
					uploadLeaseResponse.message()));
		}
		uploadFile(file, uploadLeaseResponse);
		return addFile(uploadLeaseResponse.data().leaseId(), request);
	}

	public ResponseEntity<DashScopeAPISpec.CommonResponse<DashScopeAPISpec.QueryFileResponseData>> queryFileInfo(String categoryId,
                                                                                                                 DashScopeAPISpec.UploadRequest.QueryFileRequest request) {
		return this.restClient.post()
			.uri("/api/v1/datacenter/category/{category}/file/{fileId}/query", categoryId, request.fileId())
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}

	public String getFileParseResult(String categoryId, DashScopeAPISpec.UploadRequest.QueryFileRequest request) {
		ResponseEntity<DashScopeAPISpec.CommonResponse<DashScopeAPISpec.QueryFileParseResultData>> fileParseResponse = this.restClient.post()
			.uri("/api/v1/datacenter/category/{categoryId}/file/{fileId}/download_lease", categoryId, request.fileId())
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
		if (fileParseResponse == null || fileParseResponse.getBody() == null) {
			throw new DashScopeException("GetDocumentParseResultError");
		}
		DashScopeAPISpec.CommonResponse<DashScopeAPISpec.QueryFileParseResultData> commonResponse = fileParseResponse.getBody();

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		for (String key : commonResponse.data().param().headers().keySet()) {
			headers.set(key, commonResponse.data().param().headers().get(key));
		}
		try {
			HttpEntity<InputStreamResource> requestEntity = new HttpEntity<>(null, headers);
			ResponseEntity<String> response = restTemplate.exchange(new URI(commonResponse.data().param().url()),
					HttpMethod.GET, requestEntity, String.class);
			return response.getBody();
		}
		catch (Exception ex) {
			throw new DashScopeException("GetDocumentParseResultError");
		}
	}

	private String addFile(String leaseId, DashScopeAPISpec.UploadRequest request) {
		try {
			DashScopeAPISpec.UploadRequest.AddFileRequest addFileRequest = new DashScopeAPISpec.UploadRequest.AddFileRequest(leaseId,
					DashScopeApiConstants.DEFAULT_PARSER_NAME);
			ResponseEntity<DashScopeAPISpec.CommonResponse<DashScopeAPISpec.AddFileResponseData>> response = this.restClient.post()
				.uri("/api/v1/datacenter/category/{categoryId}/add_file", request.categoryId())
				.body(addFileRequest)
				.retrieve()
				.toEntity(new ParameterizedTypeReference<>() {
				});
			DashScopeAPISpec.CommonResponse<DashScopeAPISpec.AddFileResponseData> addFileResponse = response.getBody();
			if (addFileResponse == null || !"SUCCESS".equals(addFileResponse.code().toUpperCase())) {
				throw new DashScopeException(ErrorCodeEnum.READER_ADD_FILE_ERROR);
			}
			DashScopeAPISpec.AddFileResponseData addFileResult = addFileResponse.data();
			return addFileResult.fileId();
		}
		catch (Exception ex) {
			throw new DashScopeException(ErrorCodeEnum.READER_ADD_FILE_ERROR);
		}
	}

	private void uploadFile(File file, DashScopeAPISpec.UploadLeaseResponse uploadLeaseResponse) {
		try {
			DashScopeAPISpec.UploadLeaseParamData uploadParam = uploadLeaseResponse.data().param();
			OkHttpClient client = new OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
				.writeTimeout(60, TimeUnit.SECONDS)
				.readTimeout(60, TimeUnit.SECONDS)
				.build();

			okhttp3.Headers.Builder headersBuilder = new okhttp3.Headers.Builder();
			String contentType = uploadParam.header().remove("Content-Type");

			for (String key : uploadParam.header().keySet()) {
				headersBuilder.add(key, uploadParam.header().get(key));
			}

			RequestBody requestBody;
			if (StringUtils.hasLength(contentType)) {
				requestBody = RequestBody.create(file, okhttp3.MediaType.parse(contentType));
			}
			else {
				requestBody = RequestBody.create(file, null);
				headersBuilder.add("Content-Type", "");
			}

			Request request = new Request.Builder().url(uploadParam.url())
				.headers(headersBuilder.build())
				.put(requestBody)
				.build();

			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					throw new Exception("Unexpected response code: " + response.code());
				}
			}
		}
		catch (Exception ex) {
			throw new DashScopeException("Upload File Failed", ex);
		}
	}

	private ResponseEntity<DashScopeAPISpec.UploadLeaseResponse> uploadLease(DashScopeAPISpec.UploadRequest request) {
		return this.restClient.post()
			.uri("/api/v1/datacenter/category/{categoryId}/upload_lease", request.categoryId())
			.body(request)
			.retrieve()
			.toEntity(DashScopeAPISpec.UploadLeaseResponse.class);
	}

	public ResponseEntity<DashScopeAPISpec.DocumentSplitResponse> documentSplit(Document document,
                                                                                DashScopeDocumentTransformerOptions options) {
		DashScopeAPISpec.DocumentSplitRequest request = new DashScopeAPISpec.DocumentSplitRequest(document.getText(), options.getChunkSize(),
				options.getOverlapSize(), options.getFileType(), options.getLanguage(), options.getSeparator());
		return this.restClient.post()
			.uri("/api/v1/indices/component/configed_transformations/spliter")
			.body(request)
			.retrieve()
			.toEntity(new ParameterizedTypeReference<>() {
			});
	}


	public String getPipelineIdByName(String pipelineName) {
		ResponseEntity<DashScopeAPISpec.QueryPipelineResponse> startPipelineResponse = this.restClient.get()
			.uri("/api/v1/indices/pipeline_simple?pipeline_name={pipelineName}", pipelineName)
			.retrieve()
			.toEntity(DashScopeAPISpec.QueryPipelineResponse.class);
		if (startPipelineResponse == null || startPipelineResponse.getBody() == null
				|| startPipelineResponse.getBody().pipelineId() == null) {
			return null;
		}
		return startPipelineResponse.getBody().pipelineId();
	}

	public void upsertPipeline(List<Document> documents, DashScopeStoreOptions storeOptions) {
		String embeddingModelName = (storeOptions.getEmbeddingOptions() == null ? DEFAULT_EMBEDDING_MODEL
				: storeOptions.getEmbeddingOptions().getModel());
		DashScopeAPISpec.EmbeddingConfiguredTransformations embeddingConfig = new DashScopeAPISpec.EmbeddingConfiguredTransformations(
				"DASHSCOPE_EMBEDDING",
				new DashScopeAPISpec.EmbeddingConfiguredTransformations.EmbeddingComponent(embeddingModelName));
		DashScopeDocumentTransformerOptions transformerOptions = storeOptions.getTransformerOptions();
		if (transformerOptions == null) {
			transformerOptions = new DashScopeDocumentTransformerOptions();
		}
		DashScopeAPISpec.ParserConfiguredTransformations parserConfig = new DashScopeAPISpec.ParserConfiguredTransformations(
				"DASHSCOPE_JSON_NODE_PARSER",
				new DashScopeAPISpec.ParserConfiguredTransformations.ParserComponent(
						transformerOptions.getChunkSize(), transformerOptions.getOverlapSize(), "idp",
						transformerOptions.getSeparator(), transformerOptions.getLanguage()));
		DashScopeDocumentRetrieverOptions retrieverOptions = storeOptions.getRetrieverOptions();
		if (retrieverOptions == null) {
			retrieverOptions = new DashScopeDocumentRetrieverOptions();
		}
		DashScopeAPISpec.RetrieverConfiguredTransformations retrieverConfig = new DashScopeAPISpec.RetrieverConfiguredTransformations(
				"DASHSCOPE_RETRIEVER",
				new DashScopeAPISpec.RetrieverConfiguredTransformations.RetrieverComponent(
						retrieverOptions.isEnableRewrite(),
                        List.of(new DashScopeAPISpec.RetrieverConfiguredTransformations.CommonModelComponent(
                                retrieverOptions.getRewriteModelName())),
						retrieverOptions.getSparseSimilarityTopK(), retrieverOptions.getDenseSimilarityTopK(),
						retrieverOptions.isEnableReranking(),
                        List.of(new DashScopeAPISpec.RetrieverConfiguredTransformations.CommonModelComponent(
                                retrieverOptions.getRerankModelName())),
						retrieverOptions.getRerankMinScore(), retrieverOptions.getRerankTopN(),
						retrieverOptions.getSearchFilters()));
		List<String> documentIdList = documents.stream()
			.map(Document::getId)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		DashScopeAPISpec.UpsertPipelineRequest upsertPipelineRequest = new DashScopeAPISpec.UpsertPipelineRequest(storeOptions.getIndexName(),
				"MANAGED_SHARED", null, "unstructured", "recommend",
				Arrays.asList(embeddingConfig, parserConfig, retrieverConfig),
                List.of(new DashScopeAPISpec.DataSourcesConfig("DATA_CENTER_FILE",
                        new DashScopeAPISpec.DataSourcesConfig.DataSourcesComponent(documentIdList))),
                List.of(new DashScopeAPISpec.DataSinksConfig("BUILT_IN", null))

		);
		ResponseEntity<DashScopeAPISpec.UpsertPipelineResponse> upsertPipelineResponse = this.restClient.put()
			.uri("/api/v1/indices/pipeline")
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(DashScopeAPISpec.UpsertPipelineResponse.class);
		if (upsertPipelineResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(upsertPipelineResponse.getBody().status())) {
			throw new DashScopeException(ErrorCodeEnum.CREATE_INDEX_ERROR);
		}
		String pipelineId = upsertPipelineResponse.getBody().id();
		ResponseEntity<DashScopeAPISpec.StartPipelineResponse> startPipelineResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/managed_ingest", pipelineId)
			.body(upsertPipelineRequest)
			.retrieve()
			.toEntity(DashScopeAPISpec.StartPipelineResponse.class);
		if (startPipelineResponse.getBody() == null || !"SUCCESS".equalsIgnoreCase(startPipelineResponse.getBody().code())
				|| startPipelineResponse.getBody().ingestionId() == null) {
			throw new DashScopeException(ErrorCodeEnum.INDEX_ADD_DOCUMENT_ERROR);
		}
	}

	public boolean deletePipelineDocument(String pipelineId, List<String> idList) {
		DashScopeAPISpec.DelePipelineDocumentRequest request = new DashScopeAPISpec.DelePipelineDocumentRequest(Arrays
			.asList(new DashScopeAPISpec.DelePipelineDocumentRequest.DelePipelineDocumentDataSource("DATA_CENTER_FILE",
					Arrays.asList(new DashScopeAPISpec.DelePipelineDocumentRequest.DelePipelineDocumentDataSourceComponent(idList)))));
		ResponseEntity<DashScopeAPISpec.DelePipelineDocumentResponse> deleDocumentResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/delete", pipelineId)
			.body(request)
			.retrieve()
			.toEntity(DashScopeAPISpec.DelePipelineDocumentResponse.class);
		if (deleDocumentResponse == null || deleDocumentResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(deleDocumentResponse.getBody().code())) {
			return false;
		}
		return true;
	}

	public List<Document> retriever(String pipelineId, String query, DashScopeDocumentRetrieverOptions searchOption) {
		DashScopeAPISpec.DocumentRetrieveRequest request = new DashScopeAPISpec.DocumentRetrieveRequest(query, searchOption.getDenseSimilarityTopK(),
				searchOption.getDenseSimilarityTopK(), searchOption.isEnableRewrite(),
                List.of(new DashScopeAPISpec.DocumentRetrieveRequest.DocumentRetrieveModelConfig(
                        searchOption.getRewriteModelName(), "DashScopeTextRewrite")),
				searchOption.isEnableReranking(),
                List.of(new DashScopeAPISpec.DocumentRetrieveRequest.DocumentRetrieveModelConfig(searchOption.getRerankModelName(),
                        null)),
				searchOption.getRerankMinScore(), searchOption.getRerankTopN(), searchOption.getSearchFilters());
		ResponseEntity<DashScopeAPISpec.DocumentRetrieveResponse> deleDocumentResponse = this.restClient.post()
			.uri("/api/v1/indices/pipeline/{pipeline_id}/retrieve", pipelineId)
			.body(request)
			.retrieve()
			.toEntity(DashScopeAPISpec.DocumentRetrieveResponse.class);
		if (deleDocumentResponse == null || deleDocumentResponse.getBody() == null
				|| !"SUCCESS".equalsIgnoreCase(deleDocumentResponse.getBody().code())) {
			throw new DashScopeException(ErrorCodeEnum.RETRIEVER_DOCUMENT_ERROR);
		}
		List<DashScopeAPISpec.DocumentRetrieveResponse.DocumentRetrieveResponseNode> nodeList = deleDocumentResponse.getBody().nodes();
		if (nodeList == null || nodeList.isEmpty()) {
			return new ArrayList<>();
		}
		List<Document> documents = new ArrayList<>();
		nodeList.forEach(e -> {
			DashScopeAPISpec.DocumentRetrieveResponse.DocumentRetrieveResponseNodeData nodeData = e.node();
			Document toDocument = new Document(nodeData.id(), nodeData.text(), nodeData.metadata());
			documents.add(toDocument);
		});
		return documents;
	}


	public static String getTextContent(List<DashScopeAPISpec.ChatCompletionMessage.MediaContent> content) {
		return content.stream()
			.filter(c -> "text".equals(c.type()))
			.map(DashScopeAPISpec.ChatCompletionMessage.MediaContent::text)
			.reduce("", (a, b) -> a + b);
	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @return Entity response with {@link DashScopeAPISpec.ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<DashScopeAPISpec.ChatCompletion> chatCompletionEntity(DashScopeAPISpec.ChatCompletionRequest chatRequest) {

        return chatCompletionEntity(chatRequest, new LinkedMultiValueMap<>());
	}

	/**
	 * Creates a model response for the given chat conversation.
	 * @param chatRequest The chat completion request.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Entity response with {@link DashScopeAPISpec.ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<DashScopeAPISpec.ChatCompletion> chatCompletionEntity(DashScopeAPISpec.ChatCompletionRequest chatRequest,
                                                                                MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(!chatRequest.stream(), "Request must set the stream property to false.");
		Assert.notNull(additionalHttpHeader, "The additional HTTP headers can not be null.");

		var chatCompletionUri = this.completionsPath;
		if (chatRequest.multiModel()) {
			chatCompletionUri = "/api/v1/services/aigc/multimodal-generation/generation";
		}

		// @formatter:off
		return this.restClient.post()
				.uri(chatCompletionUri)
				.headers(headers -> {
					headers.addAll(additionalHttpHeader);
					addDefaultHeadersIfMissing(headers);
				})
				.body(chatRequest)
				.retrieve()
				.toEntity(DashScopeAPISpec.ChatCompletion.class);
		// @formatter:on
	}

	private void addDefaultHeadersIfMissing(HttpHeaders headers) {

		if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !(this.apiKey instanceof NoopApiKey)) {
			headers.setBearerAuth(this.apiKey.getValue());
		}
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<DashScopeAPISpec.ChatCompletionChunk> chatCompletionStream(DashScopeAPISpec.ChatCompletionRequest chatRequest) {

		return this.chatCompletionStream(chatRequest, null);
	}

	/**
	 * Creates a streaming chat response for the given chat conversation.
	 * @param chatRequest The chat completion request. Must have the stream property set
	 * to true.
	 * @param additionalHttpHeader Optional, additional HTTP headers to be added to the
	 * request.
	 * @return Returns a {@link Flux} stream from chat completion chunks.
	 */
	public Flux<DashScopeAPISpec.ChatCompletionChunk> chatCompletionStream(DashScopeAPISpec.ChatCompletionRequest chatRequest,
                                                                           MultiValueMap<String, String> additionalHttpHeader) {

		Assert.notNull(chatRequest, "The request body can not be null.");
		Assert.isTrue(chatRequest.stream(), "Request must set the stream property to true.");

		AtomicBoolean isInsideTool = new AtomicBoolean(false);
		boolean incrementalOutput = chatRequest.parameters() != null
				&& chatRequest.parameters().incrementalOutput() != null && chatRequest.parameters().incrementalOutput();
		DashScopeAiStreamFunctionCallingHelper chunkMerger = new DashScopeAiStreamFunctionCallingHelper(
				incrementalOutput);

		var chatCompletionUri = this.completionsPath;
		if (chatRequest.multiModel()) {
			chatCompletionUri = "/api/v1/services/aigc/multimodal-generation/generation";
		}

		return this.webClient.post().uri(chatCompletionUri).headers(headers -> {
			headers.addAll(additionalHttpHeader);
			// For DashScope stream
			headers.add("X-DashScope-SSE", "enable");
			addDefaultHeadersIfMissing(headers);
		})
			.body(Mono.just(chatRequest), DashScopeAPISpec.ChatCompletionRequest.class)
			.retrieve()
			.bodyToFlux(String.class)
			.takeUntil(SSE_DONE_PREDICATE)
			.filter(SSE_DONE_PREDICATE.negate())
			.map(content -> ModelOptionsUtils.jsonToObject(content, DashScopeAPISpec.ChatCompletionChunk.class))
			.map(chunk -> {
				if (chunkMerger.isStreamingToolFunctionCall(chunk)) {
					isInsideTool.set(true);
				}
				return chunk;
			})
			.windowUntil(chunk -> {
				if (isInsideTool.get() && chunkMerger.isStreamingToolFunctionCallFinish(chunk)) {
					isInsideTool.set(false);
					return true;
				}
				return !isInsideTool.get();
			})
			.concatMapIterable(window -> {
				Mono<DashScopeAPISpec.ChatCompletionChunk> monoChunk = window.reduce(
                        new DashScopeAPISpec.ChatCompletionChunk(null, null, null, null),
						chunkMerger::merge
                );
				return List.of(monoChunk);
			})
			.flatMap(mono -> mono);
	}

	/**
	 * Creates rerank request for dashscope rerank model.
	 * @param rerankRequest The chat completion request.
	 * @return Entity response with {@link DashScopeAPISpec.ChatCompletion} as a body and HTTP status code
	 * and headers.
	 */
	public ResponseEntity<DashScopeAPISpec.RerankResponse> rerankEntity(DashScopeAPISpec.RerankRequest rerankRequest) {
		Assert.notNull(rerankRequest, "The request body can not be null.");

		return this.restClient.post()
			.uri("/api/v1/services/rerank/text-rerank/text-rerank")
			.body(rerankRequest)
			.retrieve()
			.toEntity(DashScopeAPISpec.RerankResponse.class);
	}

	String getBaseUrl() {
		return this.baseUrl;
	}

	ApiKey getApiKey() {
		return this.apiKey;
	}

	MultiValueMap<String, String> getHeaders() {
		return this.headers;
	}

	ResponseErrorHandler getResponseErrorHandler() {
		return this.responseErrorHandler;
	}

	public static class Builder {

		public Builder() {
		}

		// Copy constructor for mutate()
		public Builder(DashScopeApi api) {
			this.baseUrl = api.getBaseUrl();
			this.apiKey = api.getApiKey();
			this.headers = new LinkedMultiValueMap<>(api.getHeaders());
			this.restClientBuilder = api.restClient != null ? api.restClient.mutate() : RestClient.builder();
			this.webClientBuilder = api.webClient != null ? api.webClient.mutate() : WebClient.builder();
			this.responseErrorHandler = api.getResponseErrorHandler();
		}

		private String baseUrl = DashScopeApiConstants.DEFAULT_BASE_URL;

		private ApiKey apiKey;

		private String workSpaceId;

		private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

		private String completionsPath = "/api/v1/services/aigc/text-generation/generation";

		private String embeddingsPath = "api/v1/services/embeddings/text-embedding/text-embedding";

		private RestClient.Builder restClientBuilder = RestClient.builder();

		private WebClient.Builder webClientBuilder = WebClient.builder();

		private ResponseErrorHandler responseErrorHandler = RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER;

		public Builder baseUrl(String baseUrl) {

			Assert.notNull(baseUrl, "Base URL cannot be null");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder workSpaceId(String workSpaceId) {
			// Workspace ID is optional, but if provided, it must not be null.
			if (StringUtils.hasText(workSpaceId)) {
				Assert.notNull(workSpaceId, "Workspace ID cannot be null");
			}
			this.workSpaceId = workSpaceId;
			return this;
		}

		public Builder apiKey(String simpleApiKey) {
			Assert.notNull(simpleApiKey, "Simple api key cannot be null");
			this.apiKey = new SimpleApiKey(simpleApiKey);
			return this;
		}

		public Builder headers(MultiValueMap<String, String> headers) {
			Assert.notNull(headers, "Headers cannot be null");
			this.headers = headers;
			return this;
		}

		public Builder restClientBuilder(RestClient.Builder restClientBuilder) {
			Assert.notNull(restClientBuilder, "Rest client builder cannot be null");
			this.restClientBuilder = restClientBuilder;
			return this;
		}

		public Builder completionsPath(String completionsPath) {
			Assert.notNull(completionsPath, "Completions path cannot be null");
			this.completionsPath = completionsPath;
			return this;
		}

		public Builder embeddingsPath(String embeddingsPath) {
			Assert.notNull(embeddingsPath, "Embeddings path cannot be null");
			this.embeddingsPath = embeddingsPath;
			return this;
		}

		public Builder webClientBuilder(WebClient.Builder webClientBuilder) {
			Assert.notNull(webClientBuilder, "Web client builder cannot be null");
			this.webClientBuilder = webClientBuilder;
			return this;
		}

		public Builder responseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			Assert.notNull(responseErrorHandler, "Response error handler cannot be null");
			this.responseErrorHandler = responseErrorHandler;
			return this;
		}

		public DashScopeApi build() {

			Assert.notNull(apiKey, "API key cannot be null");

			return new DashScopeApi(this.baseUrl, this.apiKey, this.headers, this.completionsPath, this.embeddingsPath,
					// Add request header.
					this.workSpaceId, this.restClientBuilder, this.webClientBuilder, this.responseErrorHandler);
		}

	}

}
