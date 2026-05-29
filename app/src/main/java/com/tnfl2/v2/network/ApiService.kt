package com.tnfl2.v2.network

import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class SalesResponse(
    @SerializedName("data") val data: List<SaleItem>
)

data class SaleItem(
    @SerializedName("totalSalesAmount") val totalSalesAmount: Double,
    @SerializedName("profitAmount") val profitAmount: Double,
    @SerializedName("totalExpensesAmount") val totalExpensesAmount: Double,
    @SerializedName("invoiceNumber") val invoiceNumber: String,
    @SerializedName("finalCashSettlement") val finalCashSettlement: Double,
    @SerializedName("totalDigitalAmount") val totalDigitalAmount: Double,
    @SerializedName("timeCreatedAt") val timeCreatedAt: Long,
    @SerializedName("basePrice") val basePrice: Double,
    @SerializedName("kitchenSales") val kitchenSales: Double = 0.0,
    @SerializedName("_id") val id: Any? = null,
    // Speculative fields for customer data
    @SerializedName("customerName") val customerName: String? = null,
    @SerializedName("memberName") val memberName: String? = null,
    @SerializedName("customerPhone") val customerPhone: String? = null,
    @SerializedName("customer") val customer: Any? = null,
    @SerializedName("member") val member: Any? = null
)

data class EditSaleItem(
    @SerializedName("cashInHand") val cashInHand: Double,
    @SerializedName("profitAmount") val profitAmount: Double,
    @SerializedName("kitchenSales") val kitchenSales: Double,
    @SerializedName("payments") val payments: Map<String, Any>,
    @SerializedName("saleDate") val saleDate: Long,
    @SerializedName("totalSalesAmount") val totalSalesAmount: Double,
    @SerializedName("totalExpensesAmount") val totalExpensesAmount: Double,
    @SerializedName("diffSettlement") val diffSettlement: Double,
    @SerializedName("isStoreRoomAvailable") val isStoreRoomAvailable: Boolean,
    @SerializedName("invoiceNumber") val invoiceNumber: String,
    @SerializedName("finalCashSettlement") val finalCashSettlement: Double,
    @SerializedName("shopNumber") val shopNumber: String,
    @SerializedName("_id") val id: Any? = null,
    @SerializedName("expenseList") val expenseList: List<Any>,
    @SerializedName("totalDigitalAmount") val totalDigitalAmount: Double,
    @SerializedName("timeCreatedAt") val timeCreatedAt: Long,
    @SerializedName("productList") val productList: List<DraftProduct>,
    @SerializedName("openingPettyCash") val openingPettyCash: Double,
    @SerializedName("basePrice") val basePrice: Double
)

data class SingleSaleResponse(
    @SerializedName("data") val data: EditSaleItem,
    @SerializedName("status") val status: String
)

data class PerformanceDataResponse(
    @SerializedName("monthlySales") val monthlySales: List<MonthlySale>
)

data class MonthlySale(
    @SerializedName("profitAmount") val profitAmount: Double,
    @SerializedName("_id") val id: Long
)

data class ProductMasterResponse(
    @SerializedName("productList") val productList: List<Product>
)

data class Product(
    @SerializedName("SKU") val sku: String,
    @SerializedName("openingStock") val openingStock: Int,
    @SerializedName("salePrice") val salePrice: Double,
    @SerializedName("profitAmount") val profitAmount: Double,
    @SerializedName("category") val category: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("stock") val stock: Int,
    @SerializedName("purchasePrice") val purchasePrice: Double,
    @SerializedName("closingStock") val closingStock: Int
)

data class AddProductRequest(
    @SerializedName("SKU") val sku: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("category") val category: String,
    @SerializedName("details")
    val details: List<ProductDetailItem>
)

data class ProductDetailItem(
    @SerializedName("size")
    val size: String,
    @SerializedName("stock")
    val stock: Int,
    @SerializedName("purchasePrice")
    val purchasePrice: Double,
    @SerializedName("salePrice")
    val salePrice: Double,
    @SerializedName("profitAmount")
    val profitAmount: Double
)

data class DraftProduct(
    @SerializedName("SKU") val sku: String,
    @SerializedName("openingStock") val openingStock: Int,
    @SerializedName("purchaseStock") val purchaseStock: Int,
    @SerializedName("stock") val stock: Int,
    @SerializedName("closingStock") val closingStock: Int,
    @SerializedName("sales") val sales: Int,
    @SerializedName("salePrice") val salePrice: Double,
    @SerializedName("totalSaleAmount") val totalSaleAmount: Double,
    @SerializedName("category") val category: String
)

data class ConfirmSaleRequest(
    val productList: List<DraftProduct>,
    val expenseList: List<Any>,
    val payments: Map<String, Any>,
    val totalSalesAmount: Double,
    val totalExpensesAmount: Double,
    val totalDigitalAmount: Double,
    val finalCashSettlement: Double,
    val saleDate: Long,
    val openingPettyCash: Long,
    val kitchenSales: Int,
    val cashInHand: Int,
    val closingPettyCash: Int?,
    val diffSettlement: Int,
    val isStoreRoomAvailable: Boolean,
    val id: String? = null
)

data class DashboardResponse(
    @SerializedName("investmentAmount") val investmentAmount: String,
    @SerializedName("totalUnits") val totalUnits: TotalUnits
)

data class TotalUnits(
    @SerializedName("BRANDY") val brandy: Double,
    @SerializedName("VODKA") val vodka: Double,
    @SerializedName("WINE") val wine: Double,
    @SerializedName("RUM") val rum: Double,
    @SerializedName("WHISKY") val whisky: Double,
    @SerializedName("BEER") val beer: Double
)

data class ExpensesResponse(
    @SerializedName("data") val data: List<ExpenseItem>
)

data class ExpenseItem(
    @SerializedName("expenseDetail") val expenseDetail: String,
    @SerializedName("totalAmount") val totalAmount: String
)

data class PurchaseResponse(
    @SerializedName("data") val data: List<PurchaseItem>
)

data class PurchaseDraftRequest(
    @SerializedName("billNo") val billNo: String,
    @SerializedName("purchaseDate") val purchaseDate: Long,
    @SerializedName("purchaseAmount") val purchaseAmount: String,
    @SerializedName("totalQuantity") val totalQuantity: Int,
    @SerializedName("productList") val productList: List<PurchaseDraftProductItem>
)

data class GetPurchaseDraftResponse(
    @SerializedName("data") val data: List<PurchaseDraftRequest>?
)

data class PurchaseDraftProductItem(
    @SerializedName("SKU") val sku: String,
    @SerializedName("openingStock") val openingStock: Int,
    @SerializedName("purchaseStock") val purchaseStock: Int,
    @SerializedName("stock") val stock: Int,
    @SerializedName("purchaseAmount") val purchaseAmount: Double,
    @SerializedName("cases") val cases: Int
)

data class PurchaseItem(
    val purchaseDate: Long,
    val billTotalAmount: Double,
    val billTotalUnits: Int,
    val billNumber: String,
    val timeCreatedAt: Long,
    val purchaseList: List<PurchaseProduct>
)

data class PurchaseProduct(
    val openingQty: Int,
    val purchaseQty: Int,
    val purchasePrice: Double,
    @SerializedName("SKU") val sku: String,
    val lineNumber: String
)

data class StatusResponse(
    @SerializedName("status")val status: String
)

data class GetSalesDraftResponse(
    @SerializedName("data") val data: ConfirmSaleRequest?
)

interface ApiService {
    @POST("services/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("services/sales")
    suspend fun getSales(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: Long,
        @Query("endDate") endDate: Long
    ): SalesResponse

    @GET("services/sales/id")
    suspend fun getSaleById(
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): SingleSaleResponse

    @GET("services/sales") suspend fun trySalesRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/members/sales") suspend fun tryMembersSalesRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/customers/sales") suspend fun tryCustomersSalesRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/users/sales") suspend fun tryUsersSalesRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/clients/sales") suspend fun tryClientsSalesRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/transactions") suspend fun tryTransactionsRaw(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>
    @GET("services/member-sales") suspend fun tryMemberSalesRaw2(@Header("Authorization") token: String): retrofit2.Response<okhttp3.ResponseBody>

    @GET("services/dashboard/performance")
    suspend fun getPerformanceData(
        @Header("Authorization") token: String,
        @Query("startDate") startDate: Long,
        @Query("endDate") endDate: Long
    ): PerformanceDataResponse

    @GET("services/productmaster")
    suspend fun getProductMaster(@Header("Authorization") token: String): ProductMasterResponse
    
    @POST("services/productmaster")
    suspend fun addProduct(@Header("Authorization") token: String, @Body request: AddProductRequest)

    @GET("services/sales/draft")
    suspend fun getSalesDraft(@Header("Authorization") token: String): GetSalesDraftResponse

    @POST("services/sales/draft")
    suspend fun saveDraft(@Header("Authorization") token: String, @Body request: ConfirmSaleRequest): StatusResponse

    @POST("services/sales")
    suspend fun confirmSale(@Header("Authorization") token: String, @Body request: ConfirmSaleRequest)

    @PUT("services/sales")
    suspend fun updateSale(@Header("Authorization") token: String, @Body request: ConfirmSaleRequest): StatusResponse

    @GET("services/dashboard")
    suspend fun getDashboard(@Header("Authorization") token: String): DashboardResponse

    @GET("services/expenses/expensesReport")
    suspend fun getExpenses(
        @Header("Authorization") token: String,
        @Query("fromTime") fromTime: Long,
        @Query("toTime") toTime: Long
    ): ExpensesResponse

    @GET("services/purchase")
    suspend fun getPurchases(@Header("Authorization") token: String): PurchaseResponse

    @GET("services/purchase/draft")
    suspend fun getPurchaseDraft(@Header("Authorization") token: String): GetPurchaseDraftResponse

    @POST("services/purchase/draft")
    suspend fun savePurchaseDraft(@Header("Authorization") token: String, @Body request: PurchaseDraftRequest): StatusResponse

    @PUT("services/productmaster")
    suspend fun savePurchase(@Header("Authorization") token: String, @Body request: PurchaseDraftRequest): Response<ResponseBody>

    @GET("services/members")
    suspend fun getMembers(@Header("Authorization") token: String): MembersResponse

    @GET("services/members")
    suspend fun tryMembersRaw(@Header("Authorization") token: String): Response<ResponseBody>

    @GET("services/customers")
    suspend fun tryCustomersRaw(@Header("Authorization") token: String): Response<ResponseBody>

    @GET("services/users")
    suspend fun tryUsersRaw(@Header("Authorization") token: String): Response<ResponseBody>

    @GET("services/clients")
    suspend fun tryClientsRaw(@Header("Authorization") token: String): Response<ResponseBody>

    @GET("services/customer")
    suspend fun tryCustomerRaw(@Header("Authorization") token: String): Response<ResponseBody>
}

data class Member(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("name") val name: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("joinedDate") val joinedDate: String? = null,
    @SerializedName("tier") val tier: String,
    @SerializedName("totalSpend") val totalSpend: Double = 0.0,
    @SerializedName("points") val points: Int = 0
)

data class MembersResponse(
    @SerializedName("data") val data: List<Member>
)
