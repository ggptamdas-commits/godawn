package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.Transaction
import com.example.ui.viewmodel.CartItemWithProduct
import com.example.ui.viewmodel.InventoryViewModel
import com.example.ui.viewmodel.SyncState
import java.text.SimpleDateFormat
import java.util.*

enum class Screen(val titleBn: String, val titleEn: String, val icon: ImageVector) {
    Dashboard("ড্যাশবোর্ড", "Dashboard", Icons.Default.Dashboard),
    Products("পণ্য তালিকা", "Products", Icons.Default.Inventory),
    Cart("কার্ট / অর্ডার", "Cart", Icons.Default.ShoppingCart),
    Logs("হিসাব নিকাশ", "Logs", Icons.Default.Assessment),
    Sync("ক্লাউড ব্যাকআপ", "Sync", Icons.Default.CloudSync)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    // States from ViewModel
    val products by viewModel.allProducts.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItemsWithProducts.collectAsStateWithLifecycle()
    val filteredTx by viewModel.filteredTransactions.collectAsStateWithLifecycle()

    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()

    // Dialog state
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showStockInDialog by remember { mutableStateOf<Product?>(null) }
    var showAddToCartDialog by remember { mutableStateOf<Product?>(null) }
    var showEditProductDialog by remember { mutableStateOf<Product?>(null) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "রন্ধনভাণ্ডার",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "RestoGodown Inventory",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick stats header icon
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alerts",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                            val lowStockCount = products.count { it.currentStock <= it.minStockAlert }
                            Text(
                                text = "$lowStockCount টি সতর্কবার্তা",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
            ) {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (screen == Screen.Cart && cartItems.isNotEmpty()) {
                                        Badge {
                                            Text(
                                                text = cartItems.sumOf { it.cartItem.quantity.toInt() }.toString()
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.titleEn
                                )
                            }
                        },
                        label = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = screen.titleBn,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = screen.titleEn,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentScreen == Screen.Products) {
                FloatingActionButton(
                    onClick = { showAddProductDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Product")
                        Text(text = "নতুন পণ্য যোগ", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(
                        products = products,
                        transactions = transactions,
                        cartCount = cartItems.sumOf { it.cartItem.quantity.toInt() },
                        onNavigateToProducts = { currentScreen = Screen.Products },
                        onNavigateToCart = { currentScreen = Screen.Cart },
                        onNavigateToLogs = { currentScreen = Screen.Logs },
                        onNavigateToSync = { currentScreen = Screen.Sync },
                        onAddProductQuick = { showAddProductDialog = true },
                        onStockInQuick = { showStockInDialog = it },
                        onAddToCartQuick = { showAddToCartDialog = it }
                    )
                    Screen.Products -> ProductsScreen(
                        products = products,
                        onEditProduct = { showEditProductDialog = it },
                        onDeleteProduct = { viewModel.deleteProduct(it) },
                        onStockIn = { showStockInDialog = it },
                        onAddToCart = { showAddToCartDialog = it }
                    )
                    Screen.Cart -> CartScreen(
                        cartItems = cartItems,
                        viewModel = viewModel,
                        onConfirmOrder = { notes, onDone ->
                            viewModel.checkoutCart(notes) { success, message ->
                                onDone(success, message)
                            }
                        }
                    )
                    Screen.Logs -> LogsScreen(
                        filteredTx = filteredTx,
                        startDate = startDate,
                        endDate = endDate,
                        onDateRangeSelected = { start, end -> viewModel.setDateRange(start, end) },
                        onClearFilter = { viewModel.clearDateFilter() }
                    )
                    Screen.Sync -> SyncScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Dialog: Add Product
    if (showAddProductDialog) {
        ProductDialog(
            titleBn = "নতুন পণ্য যুক্ত করুন",
            titleEn = "Add New Product",
            onDismiss = { showAddProductDialog = false },
            onConfirm = { nameBn, nameEn, category, stock, unit, minAlert ->
                viewModel.addProduct(nameBn, nameEn, category, stock, unit, minAlert)
                showAddProductDialog = false
            }
        )
    }

    // Dialog: Edit Product
    showEditProductDialog?.let { product ->
        ProductDialog(
            titleBn = "পণ্য এডিট করুন",
            titleEn = "Edit Product",
            product = product,
            onDismiss = { showEditProductDialog = null },
            onConfirm = { nameBn, nameEn, category, stock, unit, minAlert ->
                viewModel.updateProduct(
                    product.copy(
                        nameBn = nameBn,
                        nameEn = nameEn,
                        category = category,
                        currentStock = stock,
                        unit = unit,
                        minStockAlert = minAlert
                    )
                )
                showEditProductDialog = null
            }
        )
    }

    // Dialog: Quick Stock In (Arrival)
    showStockInDialog?.let { product ->
        StockAdjustmentDialog(
            titleBn = "মালামাল রিসিভ / গুদামে প্রবেশ",
            titleEn = "Stock In (Receive Item)",
            product = product,
            isStockIn = true,
            onDismiss = { showStockInDialog = null },
            onConfirm = { qty, notes ->
                viewModel.stockIn(product.id, qty, notes) { success ->
                    if (success) {
                        showStockInDialog = null
                    }
                }
            }
        )
    }

    // Dialog: Quick Add To Cart
    showAddToCartDialog?.let { product ->
        StockAdjustmentDialog(
            titleBn = "কার্টে যুক্ত করুন (স্টক আউট)",
            titleEn = "Add to Cart (Stock Out)",
            product = product,
            isStockIn = false,
            onDismiss = { showAddToCartDialog = null },
            onConfirm = { qty, destination ->
                viewModel.addToCart(product.id, qty, destination) { success ->
                    if (success) {
                        showAddToCartDialog = null
                    }
                }
            }
        )
    }
}

// ==========================================
// SCREEN 1: DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    products: List<Product>,
    transactions: List<Transaction>,
    cartCount: Int,
    onNavigateToProducts: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToLogs: () -> Unit,
    onNavigateToSync: () -> Unit,
    onAddProductQuick: () -> Unit,
    onStockInQuick: (Product) -> Unit,
    onAddToCartQuick: (Product) -> Unit
) {
    val totalStockQuantity = products.sumOf { it.currentStock }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bento Header Welcome / Info
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "শুভ দিন!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Text(
                    text = "গুদাম ও কিচেন ইনভেন্টরি ট্র্যাকার সিস্টেমে আপনাকে স্বাগতম।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
                )
            }
        }

        // BENTO CARD 1: Total Stock (Col span 2, row span 1)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProducts() },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "মোট মজুদ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$totalStockQuantity টি",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Total Stock Items",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Total Stock",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // BENTO ROW 1: (Left: Kitchen Order [Taller], Right: Sector Stack)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1 (Left) - Kitchen Order
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp)
                        .clickable { onNavigateToCart() },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(0xFFFFD8E4),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = "Kitchen",
                                tint = Color(0xFF31111D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "কিচেন মেইল",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (cartCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFB3261E), CircleShape)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = cartCount.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Kitchen Order",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Column 2 (Right) - Sectors Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Male Sector Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .clickable { onNavigateToProducts() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Color(0xFFE8DEF8),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Man,
                                    contentDescription = "Male Sector",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "পুরুষ সেক্টর",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Male Sector",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Family Sector Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp)
                            .clickable { onNavigateToProducts() },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        Color(0xFFF9DEDC),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Woman,
                                    contentDescription = "Family Sector",
                                    tint = Color(0xFFB3261E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ফ্যামিলি সেক্টর",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Family Sector",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // BENTO CARD 2: Add Product Entry (Col span 2, row span 1)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddProductQuick() },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBox,
                                contentDescription = "Add Box",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "নতুন মাল এন্ট্রি",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Add New Product Entry",
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = Color.White
                    )
                }
            }
        }

        // BENTO ROW 2: (Left: Reports [Taller], Right: Firebase Backup [Taller])
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1 - Analytics / Report Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp)
                        .clickable { onNavigateToLogs() },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Analytics",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.6f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "রিপোর্ট দেখুন",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "View Detailed Reports",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Simple dynamic bar indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.66f)
                                        .fillMaxHeight()
                                        .background(
                                            MaterialTheme.colorScheme.onPrimaryContainer,
                                            CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                // Column 2 - Firebase Sync Status Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(168.dp)
                        .clickable { onNavigateToSync() }
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            val path = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = Rect(0f, 0f, this@drawBehind.size.width, this@drawBehind.size.height),
                                        cornerRadius = CornerRadius(28.dp.toPx())
                                    )
                                )
                            }
                            drawPath(
                                path = path,
                                color = Color(0xFFCAC4D0),
                                style = Stroke(
                                    width = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                                )
                            )
                        },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F0F5)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Firebase Backup Active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "ফায়ারবেজ ব্যাকআপ সচল",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Low stock title & alerts list
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "কম স্টকের মালামাল (Low Stock Alerts)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val alertList = products.filter { it.currentStock <= it.minStockAlert }
        if (alertList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFA7F3D0), Color(0xFFECFDF5))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Safe",
                            tint = Color(0xFF10B981)
                        )
                        Column {
                            Text(
                                text = "সব মালামাল পর্যাপ্ত পরিমাণে রয়েছে!",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "কোন পণ্যের বর্তমান স্টক সতর্কসীমা অতিক্রম করেনি।",
                                fontSize = 11.sp,
                                color = Color(0xFF047857)
                              )
                        }
                    }
                }
            }
        } else {
            items(alertList.take(5)) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFECACA), Color(0xFFFEF2F2))
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.displayName(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "ক্যাটাগরি: ${product.category}",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "বর্তমান স্টক: ${product.currentStock} ${product.unit}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Text(
                                    text = "(ন্যূনতম সীমা: ${product.minStockAlert})",
                                    fontSize = 10.sp,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { onStockInQuick(product) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("প্রবেশ+", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = { onAddToCartQuick(product) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3B82F6)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Add To Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionBtn(
    labelBn: String,
    labelEn: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = labelEn, tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labelBn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = labelEn,
                    fontSize = 9.sp,
                    color = color.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// SCREEN 2: PRODUCTS LIST
// ==========================================
@Composable
fun ProductsScreen(
    products: List<Product>,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onStockIn: (Product) -> Unit,
    onAddToCart: (Product) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("সব ক্যাটাগরি") }

    val categories = listOf("সব ক্যাটাগরি") + products.map { it.category }.distinct().sorted()

    val filteredProducts = products.filter { product ->
        val matchesSearch = product.nameBn.contains(searchQuery, ignoreCase = true) ||
                product.nameEn.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "সব ক্যাটাগরি" || product.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("বাংলা বা ইংরেজিতে পণ্য খুঁজুন...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Category Selection horizontal row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // List Header
        Text(
            text = "মোট পণ্য প্রাপ্তি: ${filteredProducts.size} টি",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Empty",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "কোন পণ্য খুঁজে পাওয়া যায়নি!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "নিচের (+) বাটনে চাপ দিয়ে নতুন মালামাল যুক্ত করুন।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductItemCard(
                        product = product,
                        onEdit = { onEditProduct(product) },
                        onDelete = { onDeleteProduct(product) },
                        onStockIn = { onStockIn(product) },
                        onAddToCart = { onAddToCart(product) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockIn: () -> Unit,
    onAddToCart: () -> Unit
) {
    val isLowStock = product.currentStock <= product.minStockAlert
    val cardBg = if (isLowStock) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isLowStock) CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFFFECACA), Color(0xFFFEF2F2)))) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Name and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = product.nameBn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = product.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Text(
                        text = product.nameEn,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Edit Button
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body: Stock Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "বর্তমান স্টক (Current Stock):",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${product.currentStock} ${product.unit}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isLowStock) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                        if (isLowStock) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Low",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStockIn,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Stock In",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("প্রবেশ", fontSize = 12.sp, color = Color.White)
                    }

                    Button(
                        onClick = onAddToCart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "To Cart",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কার্ট", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: CART SCREEN
// ==========================================
@Composable
fun CartScreen(
    cartItems: List<CartItemWithProduct>,
    viewModel: InventoryViewModel,
    onConfirmOrder: (String, (Boolean, String) -> Unit) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "কার্ট তালিকা (Cart items)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "মোট আইটেম: ${cartItems.size} টি",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (cartItems.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearCart() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Icon(imageVector = Icons.Default.ClearAll, contentDescription = "Clear")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সব মুছুন (Clear All)")
                }
            }
        }

        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveShoppingCart,
                        contentDescription = "Empty Cart",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "আপনার কার্ট বর্তমানে খালি আছে!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "পণ্য তালিকা থেকে গুদাম বা কিচেন মালামাল যুক্ত করুন।",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onUpdateQty = { id, qty -> viewModel.updateCartItemQty(id, qty) },
                        onRemove = { viewModel.deleteCartItem(it) }
                    )
                }
            }

            // Checkout Card Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        placeholder = { Text("প্রয়োজনীয় নোট লিখুন (যেমন: কিচেনের জরুরি মালামাল)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            onConfirmOrder(notes) { success, msg ->
                                isSuccessStatus = success
                                resultMessage = msg
                                if (success) {
                                    notes = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অর্ডার কনফার্ম করুন (Confirm Stock Out)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Success / Error Feedback Dialog
    resultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            icon = {
                Icon(
                    imageVector = if (isSuccessStatus) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = "Status",
                    tint = if (isSuccessStatus) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = if (isSuccessStatus) "অর্ডার সম্পন্ন!" else "ত্রুটি (Error!)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { resultMessage = null }
                ) {
                    Text("ঠিক আছে")
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    item: CartItemWithProduct,
    onUpdateQty: (Int, Double) -> Unit,
    onRemove: (CartItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.product.nameBn,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Destination Badge
                    val destLabel = when (item.cartItem.destination.uppercase()) {
                        "KITCHEN" -> "রান্নাঘর"
                        "FAMILY" -> "ফ্যামিলি"
                        "MALE" -> "পুরুষ সেক্টর"
                        else -> item.cartItem.destination
                    }
                    val destColor = when (item.cartItem.destination.uppercase()) {
                        "KITCHEN" -> Color(0xFFF59E0B)
                        "FAMILY" -> Color(0xFFEC4899)
                        "MALE" -> Color(0xFF3B82F6)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .background(destColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = destLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = destColor
                        )
                    }
                }
                Text(
                    text = item.product.nameEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "বর্তমান স্টক: ${item.product.currentStock} ${item.product.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Quantity adjusters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        if (item.cartItem.quantity > 1) {
                            onUpdateQty(item.cartItem.id, item.cartItem.quantity - 1)
                        } else {
                            onRemove(item.cartItem)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "${item.cartItem.quantity} ${item.product.unit}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = {
                        // Check stock upperbound
                        if (item.cartItem.quantity < item.product.currentStock) {
                            onUpdateQty(item.cartItem.id, item.cartItem.quantity + 1)
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Increase", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { onRemove(item.cartItem) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: TRANSACTION LOGS / STATS
// ==========================================
@Composable
fun LogsScreen(
    filteredTx: List<Transaction>,
    startDate: String,
    endDate: String,
    onDateRangeSelected: (String, String) -> Unit,
    onClearFilter: () -> Unit
) {
    val context = LocalContext.current

    // Helper to open android Date picker
    fun showDatePicker(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _, sYear, sMonth, sDay ->
                val formattedMonth = String.format("%02d", sMonth + 1)
                val formattedDay = String.format("%02d", sDay)
                val selectedDate = "$sYear-$formattedMonth-$formattedDay"

                if (isStart) {
                    onDateRangeSelected(selectedDate, endDate)
                } else {
                    onDateRangeSelected(startDate, selectedDate)
                }
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    // Statistics Calculations based on date/range selection
    val totalStockIn = filteredTx.filter { it.type == "STOCK_IN" }.sumOf { it.quantity }
    val totalKitchenOut = filteredTx.filter { it.type == "OUT_KITCHEN" }.sumOf { it.quantity }
    val totalFamilyOut = filteredTx.filter { it.type == "OUT_FAMILY" }.sumOf { it.quantity }
    val totalMaleOut = filteredTx.filter { it.type == "OUT_MALE" }.sumOf { it.quantity }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date Pickers Controls Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "তারিখ বা তারিখের সীমা সিলেক্ট করুন (Date Filter)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker(true) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Start Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("শুরুর তারিখ (Start)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (startDate.isNotBlank()) startDate else "সিলেক্ট করুন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // End Date Button
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showDatePicker(false) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "End Date", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("শেষের তারিখ (End)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (endDate.isNotBlank()) endDate else "সিলেক্ট করুন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    if (startDate.isNotBlank() || endDate.isNotBlank()) {
                        Button(
                            onClick = onClearFilter,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FilterAltOff, contentDescription = "Clear Filters", tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ফিল্টার মুছুন", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Summary Calculations Panel (Answers user's specific request)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "নির্বাচিত সময়ের মালামালের প্রপার হিসাব:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট মাল এসেছে (Stock In)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalStockIn একক (Units)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }

                        Column {
                            Text("মোট রান্নাঘরে গেছে (Kitchen)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalKitchenOut একক (Units)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ফ্যামিলি সেক্টরে গেছে (Family)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalFamilyOut একক (Units)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                        }

                        Column {
                            Text("পুরুষ সেক্টরে গেছে (Men)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalMaleOut একক (Units)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
                        }
                    }
                }
            }
        }

        // List Header
        item {
            Text(
                text = "লেনদেন রেকর্ড সমূহ (${filteredTx.size} টি)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (filteredTx.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "No info", modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("এই সময়ে কোন লেনদেনের হিসাব পাওয়া যায়নি!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(filteredTx) { tx ->
                TransactionRowCard(tx = tx)
            }
        }
    }
}

@Composable
fun TransactionRowCard(tx: Transaction) {
    val txColor = when (tx.type) {
        "STOCK_IN" -> Color(0xFF10B981)
        "OUT_KITCHEN" -> Color(0xFFF59E0B)
        "OUT_FAMILY" -> Color(0xFFEC4899)
        "OUT_MALE" -> Color(0xFF3B82F6)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Color Stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(txColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tx.productNameBn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .background(txColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = tx.getDestinationLabel(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = txColor
                        )
                    }
                }
                Text(
                    text = tx.productNameEn,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tx.notes.isNotBlank()) {
                    Text(
                        text = "মন্তব্য: ${tx.notes}",
                        fontSize = 11.sp,
                        color = Color(0xFF4B5563),
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "তারিখ: ${tx.date}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Quantity column
            val signPrefix = if (tx.type == "STOCK_IN") "+" else "-"
            Text(
                text = "$signPrefix${tx.quantity}",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = txColor
            )
        }
    }
}

// ==========================================
// SCREEN 5: CLOUD SYNC & BACKUP
// ==========================================
@Composable
fun SyncScreen(viewModel: InventoryViewModel) {
    var dbUrl by remember { mutableStateOf(viewModel.firebaseDbUrl) }
    var projectId by remember { mutableStateOf(viewModel.firebaseProjectId) }
    var apiKey by remember { mutableStateOf(viewModel.firebaseApiKey) }
    var deviceName by remember { mutableStateOf(viewModel.deviceName) }

    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    var configSaveSuccess by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome/Instructions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                        Text(
                            text = "ক্লাউড সিঙ্ক্রোনাইজেশন ও ব্যাকআপ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "আপনার ফোনের ডাটা নিরাপদ রাখতে এবং হারিয়ে গেলেও উদ্ধার করতে ফায়ারবেইজ ডাটাবেজের ব্যাকআপ সেটিংস কনফিগার করুন।",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Configuration Settings Card Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "ফায়ারবেইজ সেটিংস (Firebase Setup Form)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = dbUrl,
                        onValueChange = { dbUrl = it },
                        label = { Text("ফায়ারবেইজ ডাটাবেজ URL (Firebase Database URL)") },
                        placeholder = { Text("https://yourproject-rtdb.firebaseio.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = projectId,
                        onValueChange = { projectId = it },
                        label = { Text("প্রজেক্ট আইডি (Project ID)") },
                        placeholder = { Text("yourproject-1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("এপিআই কি (API Key - ঐচ্ছিক)") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("ডিভাইস নাম/আইডেন্টিফায়ার (Device Identifier)") },
                        placeholder = { Text("গুদাম মোবাইল ১ (Warehouse Mobile)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.saveCloudConfig(dbUrl, projectId, apiKey, deviceName)
                            configSaveSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.align(Alignment.End),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সেটিংস সেভ করুন (Save Settings)")
                    }
                }
            }
        }

        // Backup and Restore Actions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ম্যানুয়াল ক্লাউড ব্যাকআপ অপশন",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val isConfigured = dbUrl.isNotBlank() && projectId.isNotBlank()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.backupToCloud() },
                            enabled = isConfigured && syncState !is SyncState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = "Backup")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্লাউড ব্যাকআপ নিন", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.restoreFromCloud() },
                            enabled = isConfigured && syncState !is SyncState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = "Restore")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ক্লাউড থেকে রিস্টোর", fontSize = 12.sp, color = Color.White)
                        }
                    }

                    // Loading/Success Indicators
                    when (val state = syncState) {
                        SyncState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ফায়ারবেইজ সার্ভারের সাথে সিঙ্ক হচ্ছে...", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        is SyncState.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF10B981))
                                    Text(text = state.message, fontSize = 11.sp, color = Color(0xFF047857), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        is SyncState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Error, contentDescription = "Error", tint = Color(0xFFEF4444))
                                    Text(text = state.message, fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        SyncState.Idle -> {
                            if (!isConfigured) {
                                Text(
                                    text = "⚠️ ক্লাউড ব্যাকআপ নেওয়ার পূর্বে উপরে ফায়ারবেইজ কনফিগারেশন সেভ করুন।",
                                    fontSize = 11.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Saved confirmation feedback Dialog
    if (configSaveSuccess) {
        AlertDialog(
            onDismissRequest = { configSaveSuccess = false },
            icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Saved", tint = Color(0xFF10B981)) },
            title = { Text("সেটিংস সংরক্ষিত হয়েছে!", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = { Text("আপনার ফায়ারবেইজ সেটিংস সফলভাবে সংরক্ষণ করা হয়েছে। এখন আপনি ম্যানুয়াল ব্যাকআপ নিতে পারবেন।") },
            confirmButton = {
                Button(onClick = { configSaveSuccess = false }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }
}

// ==========================================
// COMPONENT DIALOGS & SHARDS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDialog(
    titleBn: String,
    titleEn: String,
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (nameBn: String, nameEn: String, category: String, stock: Double, unit: String, minAlert: Double) -> Unit
) {
    var nameBn by remember { mutableStateOf(product?.nameBn ?: "") }
    var nameEn by remember { mutableStateOf(product?.nameEn ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "grocery") }
    var stockText by remember { mutableStateOf(product?.currentStock?.toString() ?: "0") }
    var unit by remember { mutableStateOf(product?.unit ?: "Kg") }
    var minAlertText by remember { mutableStateOf(product?.minStockAlert?.toString() ?: "5") }

    val categories = listOf("Grocery", "Vegetables", "Spices", "Oils", "Meat", "Dry Goods", "Others")
    val units = listOf("Kg", "Litre", "Piece", "Bag", "Box")

    var isCatExpanded by remember { mutableStateOf(false) }
    var isUnitExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column {
                        Text(text = titleBn, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = titleEn, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                item {
                    OutlinedTextField(
                        value = nameBn,
                        onValueChange = { nameBn = it },
                        label = { Text("বাংলা নাম (Product Name Bangla)") },
                        placeholder = { Text("যেমন: সয়াবিন তেল") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = nameEn,
                        onValueChange = { nameEn = it },
                        label = { Text("ইংরেজি নাম (Product Name English)") },
                        placeholder = { Text("e.g. Soyabean Oil") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    // Category dropdown
                    ExposedDropdownMenuBox(
                        expanded = isCatExpanded,
                        onExpandedChange = { isCatExpanded = !isCatExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("ক্যাটাগরি সিলেক্ট করুন (Category)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isCatExpanded,
                            onDismissRequest = { isCatExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        isCatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = stockText,
                            onValueChange = { stockText = it },
                            label = { Text("স্টক (Quantity)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Unit selection dropdown
                        Box(modifier = Modifier.weight(1f)) {
                            ExposedDropdownMenuBox(
                                expanded = isUnitExpanded,
                                onExpandedChange = { isUnitExpanded = !isUnitExpanded }
                            ) {
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("একক (Unit)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                ExposedDropdownMenu(
                                    expanded = isUnitExpanded,
                                    onDismissRequest = { isUnitExpanded = false }
                                ) {
                                    units.forEach { u ->
                                        DropdownMenuItem(
                                            text = { Text(u) },
                                            onClick = {
                                                unit = u
                                                isUnitExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = minAlertText,
                        onValueChange = { minAlertText = it },
                        label = { Text("ন্যূনতম সতর্কসীমা (Low Stock Limit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("বাতিল করুন (Cancel)")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val stockVal = stockText.toDoubleOrNull() ?: 0.0
                                val minVal = minAlertText.toDoubleOrNull() ?: 5.0
                                if (nameBn.isNotBlank() || nameEn.isNotBlank()) {
                                    onConfirm(nameBn, nameEn, category, stockVal, unit, minVal)
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("নিশ্চিত করুন (Confirm)")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDialog(
    titleBn: String,
    titleEn: String,
    product: Product,
    isStockIn: Boolean, // True = Stock In, False = Stock Out Add to Cart
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double, extraString: String) -> Unit // extraString can be notes or destination
) {
    var quantityText by remember { mutableStateOf("1") }
    var extraInputText by remember { mutableStateOf("") } // notes for Stock In
    var selectedDestination by remember { mutableStateOf("KITCHEN") } // Destination for Stock Out

    val destinations = listOf(
        Pair("KITCHEN", "রান্নাঘর (Kitchen)"),
        Pair("FAMILY", "ফ্যামিলি সেক্টর (Family Section)"),
        Pair("MALE", "পুরুষ সেক্টর (Men's Section)")
    )
    var isDestExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(text = titleBn, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text(text = titleEn, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                // Product details preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = product.displayName(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(text = "ক্যাটাগরি: ${product.category}", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "বর্তমান স্টক", fontSize = 10.sp)
                            Text(text = "${product.currentStock} ${product.unit}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("পরিমাণ (Quantity in ${product.unit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (isStockIn) {
                    // Notes Input
                    OutlinedTextField(
                        value = extraInputText,
                        onValueChange = { extraInputText = it },
                        label = { Text("মন্তব্য/নোট (Notes - optional)") },
                        placeholder = { Text("যেমন: বাজার থেকে মাল কেনা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else {
                    // Destination Selection Dropdown for Cart Item Outgoing
                    ExposedDropdownMenuBox(
                        expanded = isDestExpanded,
                        onExpandedChange = { isDestExpanded = !isDestExpanded }
                    ) {
                        OutlinedTextField(
                            value = destinations.find { it.first == selectedDestination }?.second ?: selectedDestination,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("কোথায় পাঠাতে চান? (Select Destination)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDestExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isDestExpanded,
                            onDismissRequest = { isDestExpanded = false }
                        ) {
                            destinations.forEach { dest ->
                                DropdownMenuItem(
                                    text = { Text(dest.second) },
                                    onClick = {
                                        selectedDestination = dest.first
                                        isDestExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল করুন")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val qty = quantityText.toDoubleOrNull() ?: 1.0
                            if (isStockIn) {
                                onConfirm(qty, extraInputText)
                            } else {
                                onConfirm(qty, selectedDestination)
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("নিশ্চিত করুন")
                    }
                }
            }
        }
    }
}
