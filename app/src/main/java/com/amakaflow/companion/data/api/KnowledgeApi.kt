package com.amakaflow.companion.data.api

import com.amakaflow.companion.data.model.KnowledgeCard
import com.amakaflow.companion.data.model.KnowledgeCardListResponse
import com.amakaflow.companion.data.model.KnowledgeIngestRequest
import com.amakaflow.companion.data.model.KnowledgeSearchRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface KnowledgeApi {
    @GET("api/knowledge/cards")
    suspend fun listCards(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
    ): KnowledgeCardListResponse

    @POST("api/knowledge/search")
    suspend fun searchCards(@Body request: KnowledgeSearchRequest): KnowledgeCardListResponse

    @POST("api/knowledge/ingest")
    suspend fun ingest(@Body request: KnowledgeIngestRequest): KnowledgeCard

    @DELETE("api/knowledge/cards/{id}")
    suspend fun deleteCard(@Path("id") id: String): Response<Unit>
}
