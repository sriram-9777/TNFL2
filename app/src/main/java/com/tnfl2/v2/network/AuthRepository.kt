package com.tnfl2.v2.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.tnfl2.v2.SessionManager

class AuthRepository private constructor() {

    val apiService: ApiService = SharedNetwork.apiService

    companion object {
        // Single shared instance for the entire app
        val instance: AuthRepository by lazy { AuthRepository() }

        // Convenience constructor for backward compatibility
        operator fun invoke(): AuthRepository = instance
    }

    private suspend fun <T> wrapWithLoading(block: suspend () -> Result<T>): Result<T> {
        com.tnfl2.v2.SessionManager.incrementLoading()
        return try {
            block()
        } finally {
            com.tnfl2.v2.SessionManager.decrementLoading()
        }
    }

    suspend fun login(request: LoginRequest): Result<LoginResponse> = wrapWithLoading {
        try {
            val response = apiService.login(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSales(token: String, startDate: Long, endDate: Long): Result<SalesResponse> = wrapWithLoading {
        try {
            val response = apiService.getSales("Bearer $token", startDate, endDate)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSaleById(token: String, id: String): Result<SingleSaleResponse> = wrapWithLoading {
        try {
            val cleanId = id.replace("{\$oid=", "").replace("}", "")
            val response = apiService.getSaleById("Bearer $token", cleanId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductMaster(token: String): Result<ProductMasterResponse> = wrapWithLoading {
        try {
            val response = apiService.getProductMaster("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProduct(token: String, request: AddProductRequest): Result<Unit> = wrapWithLoading {
        try {
            apiService.addProduct("Bearer $token", request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(token: String, request: AddProductRequest): Result<Response<ResponseBody>> = wrapWithLoading {
        try {
            val response = apiService.updateProduct("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSalesDraft(token: String): Result<GetSalesDraftResponse> = wrapWithLoading {
        try {
            val response = apiService.getSalesDraft("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveDraft(token: String, request: ConfirmSaleRequest): Result<StatusResponse> = wrapWithLoading {
        try {
            val response = apiService.saveDraft("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmSale(token: String, request: ConfirmSaleRequest): Result<Unit> = wrapWithLoading {
        try {
            apiService.confirmSale("Bearer $token", request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSale(token: String, request: ConfirmSaleRequest): Result<StatusResponse> = wrapWithLoading {
        try {
            val response = apiService.updateSale("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDashboard(token: String): Result<DashboardResponse> = wrapWithLoading {
        try {
            val response = apiService.getDashboard("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpenses(token: String, startDate: Long, endDate: Long): Result<ExpensesResponse> = wrapWithLoading {
        try {
            val response = apiService.getExpenses("Bearer $token", startDate, endDate)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPurchases(token: String): Result<PurchaseResponse> = wrapWithLoading {
        try {
            val response = apiService.getPurchases("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPurchaseDraft(token: String): Result<GetPurchaseDraftResponse> = wrapWithLoading {
        try {
            val response = apiService.getPurchaseDraft("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePurchaseDraft(token: String, request: PurchaseDraftRequest): Result<StatusResponse> = wrapWithLoading {
        try {
            val response = apiService.savePurchaseDraft("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePurchase(token: String, request: PurchaseDraftRequest): Result<Response<ResponseBody>> = wrapWithLoading {
        try {
            val response = apiService.savePurchase("Bearer $token", request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMembers(token: String): Result<MembersResponse> = wrapWithLoading {
        try {
            val response = apiService.getMembers("Bearer $token")
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
