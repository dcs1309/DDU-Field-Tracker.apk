package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.FieldIntelligenceViewModel
import kotlin.math.hypot

enum class NodeType(val label: String, val color: Color, val containerColor: Color) {
    INSTITUTION("Institution", Color(0xFFD4823A), Color(0xFFFBF1E6)),
    LOCAL_SHOP("Local Shop", Color(0xFF266333), Color(0xFFE4EDE2)),
    PRODUCT("Product Demand", Color(0xFF1E568A), Color(0xFFEAF1F8)),
    EXTERNAL_SUPPLIER("External Supplier", Color(0xFFDC5B38), Color(0xFFFBECE8)),
    DDU_OPPORTUNITY("DDU Opportunity", Color(0xFF6B3A82), Color(0xFFF4ECF7))
}

data class NetworkNode(
    val id: String,
    val name: String,
    val subtext: String,
    val type: NodeType,
    val cluster: String, // "TEXTILE", "HEALTHCARE", "FOOD", "AGRI"
    val x: Float, // Normalized 0.0 to 1.0 on canvas
    val y: Float, // Normalized 0.0 to 1.0 on canvas
    val metrics: String,
    val leakagePercent: Int = 0,
    val annualValue: String = "",
    val oppId: String? = null
)

data class NetworkEdge(
    val fromId: String,
    val toId: String,
    val label: String,
    val edgeType: EdgeType,
    val volume: String
)

enum class EdgeType {
    DEMAND_FLOW,       // Buyer -> Product
    EXTERNAL_LEAKAGE,  // Product -> External Supplier
    OPPORTUNITY_LINK   // Product -> DDU Opportunity
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpportunityNetworkGraphScreen(
    viewModel: FieldIntelligenceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOpportunity: (String) -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCluster by remember { mutableStateOf("ALL") }
    var selectedNodeId by remember { mutableStateOf<String?>("opp_uniform") }
    var viewMode by remember { mutableStateOf("GRAPH") } // "GRAPH" or "CLUSTERS"

    // Seed ecosystem network nodes
    val allNodes = remember {
        listOf(
            // Institutions & Buyers (Column 1)
            NetworkNode("inst_school", "Balrampur Govt High", "School • 850 Students", NodeType.INSTITUTION, "TEXTILE", 0.12f, 0.18f, "Annual Demand: 850 Sets", 100, "₹3.2 Lakhs"),
            NetworkNode("inst_hostel", "KGBV Girls Hostel", "Hostel • 220 Inmates", NodeType.INSTITUTION, "TEXTILE", 0.12f, 0.38f, "Annual Demand: 440 Sets", 100, "₹1.7 Lakhs"),
            NetworkNode("inst_hospital", "Balrampur CHC Hospital", "PHC • 30 Beds", NodeType.INSTITUTION, "HEALTHCARE", 0.12f, 0.58f, "Monthly Demand: 120 Sheets", 100, "₹2.8 Lakhs"),
            NetworkNode("shop_kirana", "Maa Tara General Store", "Kirana • Rampur Tola", NodeType.LOCAL_SHOP, "FOOD", 0.12f, 0.78f, "Weekly Sales: 350 Breads", 95, "₹4.8 Lakhs"),
            NetworkNode("inst_college", "Balrampur College", "College • 1200 Students", NodeType.INSTITUTION, "HEALTHCARE", 0.12f, 0.94f, "Monthly Demand: 80L Phynyl", 80, "₹1.4 Lakhs"),

            // Demanded Products (Column 2)
            NetworkNode("prod_uniform", "School Uniforms", "Standard 2-Piece Sets", NodeType.PRODUCT, "TEXTILE", 0.42f, 0.26f, "Total Demand: 2,100 Sets/yr", 100, "₹7.9 Lakhs"),
            NetworkNode("prod_linen", "Hospital Linen", "Bedsheets & Patient Gowns", NodeType.PRODUCT, "HEALTHCARE", 0.42f, 0.52f, "Total Demand: 1,440 Pcs/yr", 100, "₹3.4 Lakhs"),
            NetworkNode("prod_phynyl", "Liquid Cleaning Phynyl", "Disinfectants & Chemicals", NodeType.PRODUCT, "HEALTHCARE", 0.42f, 0.68f, "Total Demand: 600 Litres/mo", 90, "₹4.6 Lakhs"),
            NetworkNode("prod_bread", "Packaged Breads & Buns", "Fresh Bakery Supplies", NodeType.PRODUCT, "FOOD", 0.42f, 0.86f, "Total Demand: 1,800 Pkts/wk", 95, "₹9.2 Lakhs"),

            // External Suppliers / Leakage (Column 3 - Top/Bottom)
            NetworkNode("sup_tezpur", "City Garments Depot", "Tezpur Wholesaler (Outside)", NodeType.EXTERNAL_SUPPLIER, "TEXTILE", 0.72f, 0.18f, "Current Sourcing Share: 85%", 100, "₹6.8 Lakhs"),
            NetworkNode("sup_guwahati", "Guwahati Medico Chem", "Guwahati Depot (Outside)", NodeType.EXTERNAL_SUPPLIER, "HEALTHCARE", 0.72f, 0.48f, "Current Sourcing Share: 90%", 100, "₹3.1 Lakhs"),
            NetworkNode("sup_dist_bakery", "Regional Bread Factory", "Dist HQ (35km Away)", NodeType.EXTERNAL_SUPPLIER, "FOOD", 0.72f, 0.78f, "Current Sourcing Share: 95%", 100, "₹8.7 Lakhs"),

            // DDU Opportunities (Column 4 - Right)
            NetworkNode("opp_uniform", "OPP-024: Uniform Stitching", "DDU Cluster • Rampur Tola", NodeType.DDU_OPPORTUNITY, "TEXTILE", 0.90f, 0.28f, "Feasibility: High (94%)", 0, "₹7.8 Lakhs", "OPP-024"),
            NetworkNode("opp_linen", "OPP-018: Linen Aggregation", "DDU Unit • Juri Basti", NodeType.DDU_OPPORTUNITY, "HEALTHCARE", 0.90f, 0.54f, "Feasibility: High (88%)", 0, "₹3.4 Lakhs", "OPP-018"),
            NetworkNode("opp_phynyl", "OPP-012: Cleaning Chemicals", "SHG Unit • Grazi", NodeType.DDU_OPPORTUNITY, "HEALTHCARE", 0.90f, 0.68f, "Feasibility: Med (76%)", 0, "₹4.6 Lakhs", "OPP-012"),
            NetworkNode("opp_bakery", "OPP-009: Rural Micro-Bakery", "Youth Enterprise • Bazar", NodeType.DDU_OPPORTUNITY, "FOOD", 0.90f, 0.86f, "Feasibility: High (91%)", 0, "₹9.2 Lakhs", "OPP-009")
        )
    }

    val allEdges = remember {
        listOf(
            // Institution -> Product Demands
            NetworkEdge("inst_school", "prod_uniform", "Demands 850 sets", EdgeType.DEMAND_FLOW, "850 Sets/yr"),
            NetworkEdge("inst_hostel", "prod_uniform", "Demands 440 sets", EdgeType.DEMAND_FLOW, "440 Sets/yr"),
            NetworkEdge("inst_hospital", "prod_linen", "Demands 120 pcs/mo", EdgeType.DEMAND_FLOW, "1,440 Pcs/yr"),
            NetworkEdge("inst_hospital", "prod_phynyl", "Demands 50L/mo", EdgeType.DEMAND_FLOW, "600 L/yr"),
            NetworkEdge("inst_college", "prod_phynyl", "Demands 80L/mo", EdgeType.DEMAND_FLOW, "960 L/yr"),
            NetworkEdge("shop_kirana", "prod_bread", "Sells 350 pkts/wk", EdgeType.DEMAND_FLOW, "18,200 Pkts/yr"),

            // Products -> External Leakage
            NetworkEdge("prod_uniform", "sup_tezpur", "85% Leakage (Outside Block)", EdgeType.EXTERNAL_LEAKAGE, "₹6.8 Lakhs"),
            NetworkEdge("prod_linen", "sup_guwahati", "90% Leakage (Outside Block)", EdgeType.EXTERNAL_LEAKAGE, "₹3.1 Lakhs"),
            NetworkEdge("prod_phynyl", "sup_guwahati", "80% Leakage (Outside Block)", EdgeType.EXTERNAL_LEAKAGE, "₹3.6 Lakhs"),
            NetworkEdge("prod_bread", "sup_dist_bakery", "95% Leakage (Outside Block)", EdgeType.EXTERNAL_LEAKAGE, "₹8.7 Lakhs"),

            // Products -> DDU Identified Opportunities
            NetworkEdge("prod_uniform", "opp_uniform", "Direct Opportunity Link", EdgeType.OPPORTUNITY_LINK, "₹7.8 Lakhs"),
            NetworkEdge("prod_linen", "opp_linen", "Direct Opportunity Link", EdgeType.OPPORTUNITY_LINK, "₹3.4 Lakhs"),
            NetworkEdge("prod_phynyl", "opp_phynyl", "Direct Opportunity Link", EdgeType.OPPORTUNITY_LINK, "₹4.6 Lakhs"),
            NetworkEdge("prod_bread", "opp_bakery", "Direct Opportunity Link", EdgeType.OPPORTUNITY_LINK, "₹9.2 Lakhs")
        )
    }

    // Filter nodes by cluster
    val visibleNodes = remember(selectedCluster) {
        if (selectedCluster == "ALL") allNodes
        else allNodes.filter { it.cluster == selectedCluster }
    }

    val visibleNodeIds = remember(visibleNodes) { visibleNodes.map { it.id }.toSet() }

    val visibleEdges = remember(visibleNodeIds) {
        allEdges.filter { it.fromId in visibleNodeIds && it.toId in visibleNodeIds }
    }

    val selectedNode = remember(selectedNodeId) {
        allNodes.firstOrNull { it.id == selectedNodeId }
    }

    // Connected edges and nodes for selectedNode
    val connectedEdges = remember(selectedNodeId) {
        if (selectedNodeId == null) emptyList()
        else allEdges.filter { it.fromId == selectedNodeId || it.toId == selectedNodeId }
    }

    val connectedNodeIds = remember(connectedEdges, selectedNodeId) {
        if (selectedNodeId == null) emptySet()
        else connectedEdges.flatMap { listOf(it.fromId, it.toId) }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Opportunity Network Graph",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Market Ecosystem & Supply-Demand Clusters",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("network_graph_back_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewMode = if (viewMode == "GRAPH") "CLUSTERS" else "GRAPH" },
                        modifier = Modifier.testTag("toggle_view_mode_button")
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (viewMode == "GRAPH") Icons.Default.ListAlt else Icons.Default.Hub,
                                    contentDescription = "Toggle View",
                                    tint = DduPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DduBackground)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(DduBackground)
                .padding(innerPadding)
        ) {
            // Cluster Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val clusters = listOf(
                    "ALL" to "All Ecosystems (16)",
                    "TEXTILE" to "Textiles & Garments",
                    "HEALTHCARE" to "Health & Hygiene",
                    "FOOD" to "Bakery & Food Processing"
                )
                items(clusters) { (key, label) ->
                    val isSelected = selectedCluster == key
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedCluster = key
                            // Select first node in cluster
                            val firstInCluster = allNodes.firstOrNull { key == "ALL" || it.cluster == key }
                            selectedNodeId = firstInCluster?.id
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DduPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("cluster_chip_$key")
                    )
                }
            }

            // Top Metrics Strip
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GraphStatItem(title = "Total Demand", value = "₹30.8L/yr", color = Color(0xFF1565C0))
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    GraphStatItem(title = "Outside Leakage", value = "84%", color = Color(0xFFC62828))
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    GraphStatItem(title = "DDU Pipeline", value = "₹25.0L", color = Color(0xFF7B1FA2))
                    Divider(modifier = Modifier.height(24.dp).width(1.dp))
                    GraphStatItem(title = "Enterprises", value = "4 Active", color = Color(0xFF00796B))
                }
            }

            if (viewMode == "GRAPH") {
                // Interactive Graph Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF0F172A)) // High-contrast dark graph canvas
                ) {
                    NetworkGraphCanvas(
                        nodes = visibleNodes,
                        edges = visibleEdges,
                        selectedNodeId = selectedNodeId,
                        connectedNodeIds = connectedNodeIds,
                        onNodeTapped = { node -> selectedNodeId = node.id }
                    )

                    // Canvas Legend Overlay
                    Surface(
                        color = Color(0xCC1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            GraphLegendItem(color = Color(0xFF00796B), label = "Institution")
                            Spacer(modifier = Modifier.height(3.dp))
                            GraphLegendItem(color = Color(0xFF1565C0), label = "Product Demand")
                            Spacer(modifier = Modifier.height(3.dp))
                            GraphLegendItem(color = Color(0xFFC62828), label = "External Sourcing (Leakage)")
                            Spacer(modifier = Modifier.height(3.dp))
                            GraphLegendItem(color = Color(0xFF7B1FA2), label = "DDU Opportunity")
                        }
                    }

                    // Instruction prompt
                    Text(
                        text = "Tap any node to trace supply-demand pathways",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            } else {
                // Clusters List Matrix View
                ClustersMatrixList(
                    nodes = visibleNodes,
                    edges = allEdges,
                    selectedNodeId = selectedNodeId,
                    onSelectNode = { selectedNodeId = it },
                    onNavigateToOpportunity = onNavigateToOpportunity,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom Selected Node Inspector Sheet
            if (selectedNode != null) {
                NodeInspectorCard(
                    node = selectedNode,
                    edges = connectedEdges,
                    allNodes = allNodes,
                    onNavigateToOpportunity = onNavigateToOpportunity,
                    onNavigateToRecordDetail = onNavigateToRecordDetail,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun NetworkGraphCanvas(
    nodes: List<NetworkNode>,
    edges: List<NetworkEdge>,
    selectedNodeId: String?,
    connectedNodeIds: Set<String>,
    onNodeTapped: (NetworkNode) -> Unit
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(nodes) {
                detectTapGestures { tapOffset ->
                    if (canvasSize.width <= 0 || canvasSize.height <= 0) return@detectTapGestures
                    // Find node closest to tap
                    val tapped = nodes.minByOrNull { node ->
                        val nodeX = node.x * canvasSize.width
                        val nodeY = node.y * canvasSize.height
                        hypot(nodeX - tapOffset.x, nodeY - tapOffset.y)
                    }
                    if (tapped != null) {
                        val nodeX = tapped.x * canvasSize.width
                        val nodeY = tapped.y * canvasSize.height
                        if (hypot(nodeX - tapOffset.x, nodeY - tapOffset.y) < 55f) {
                            onNodeTapped(tapped)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            canvasSize = size

            val nodeMap = nodes.associateBy { it.id }

            // 1. Draw Edges
            edges.forEach { edge ->
                val from = nodeMap[edge.fromId]
                val to = nodeMap[edge.toId]
                if (from != null && to != null) {
                    val startX = from.x * size.width
                    val startY = from.y * size.height
                    val endX = to.x * size.width
                    val endY = to.y * size.height

                    val isConnected = selectedNodeId == null ||
                        (edge.fromId == selectedNodeId || edge.toId == selectedNodeId)

                    val edgeColor = when (edge.edgeType) {
                        EdgeType.DEMAND_FLOW -> if (isConnected) Color(0xFF64B5F6) else Color(0x2564B5F6)
                        EdgeType.EXTERNAL_LEAKAGE -> if (isConnected) Color(0xFFEF5350) else Color(0x25EF5350)
                        EdgeType.OPPORTUNITY_LINK -> if (isConnected) Color(0xFFCE93D8) else Color(0x25CE93D8)
                    }

                    val strokeWidth = if (isConnected) 3.5f else 1.2f
                    val pathEffect = if (edge.edgeType == EdgeType.EXTERNAL_LEAKAGE) {
                        PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    } else null

                    drawLine(
                        color = edgeColor,
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = strokeWidth,
                        pathEffect = pathEffect
                    )

                    // Draw volume / badge at edge midpoint if active
                    if (isConnected && selectedNodeId != null) {
                        val midX = (startX + endX) / 2
                        val midY = (startY + endY) / 2
                        drawCircle(
                            color = edgeColor.copy(alpha = 0.8f),
                            radius = 4f,
                            center = Offset(midX, midY)
                        )
                    }
                }
            }

            // 2. Draw Nodes
            nodes.forEach { node ->
                val cx = node.x * size.width
                val cy = node.y * size.height

                val isSelected = node.id == selectedNodeId
                val isConnected = selectedNodeId == null || node.id in connectedNodeIds
                val alpha = if (isSelected) 1f else if (isConnected) 0.9f else 0.25f

                val baseColor = node.type.color

                // Outer selection halo
                if (isSelected) {
                    drawCircle(
                        color = baseColor.copy(alpha = 0.35f),
                        radius = 28f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 22f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5f)
                    )
                }

                // Core Node circle
                drawCircle(
                    color = baseColor.copy(alpha = alpha),
                    radius = if (isSelected) 18f else 14f,
                    center = Offset(cx, cy)
                )

                // White inner dot
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 5f,
                    center = Offset(cx, cy)
                )

                // Draw Text Label on native canvas
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.LTGRAY
                        textSize = if (isSelected) 24f else 20f
                        isFakeBoldText = isSelected
                        isAntiAlias = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                    val labelY = if (cy < size.height * 0.5f) cy + 34f else cy - 24f
                    drawText(node.name, cx - (node.name.length * 5.5f), labelY, paint)
                }
            }
        }
    }
}

@Composable
fun NodeInspectorCard(
    node: NetworkNode,
    edges: List<NetworkEdge>,
    allNodes: List<NetworkNode>,
    onNavigateToOpportunity: (String) -> Unit,
    onNavigateToRecordDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val nodeMap = remember(allNodes) { allNodes.associateBy { it.id } }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(node.type.color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.type.label.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = node.type.color
                    )
                }

                if (node.oppId != null) {
                    Button(
                        onClick = { onNavigateToOpportunity(node.oppId) },
                        colors = ButtonDefaults.buttonColors(containerColor = DduPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("inspector_open_opp_button")
                    ) {
                        Text("Open ${node.oppId}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (node.type == NodeType.INSTITUTION || node.type == NodeType.LOCAL_SHOP) {
                    OutlinedButton(
                        onClick = { onNavigateToRecordDetail("DDU-BAL-2026-000124") },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("View Survey", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = node.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${node.subtext} • ${node.metrics}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Connected Pathways
            Text(
                text = "Connected Supply-Demand Pathways (${edges.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            edges.take(3).forEach { edge ->
                val otherNodeId = if (edge.fromId == node.id) edge.toId else edge.fromId
                val otherNode = nodeMap[otherNodeId]
                if (otherNode != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (edge.edgeType) {
                                    EdgeType.DEMAND_FLOW -> Icons.Default.TrendingUp
                                    EdgeType.EXTERNAL_LEAKAGE -> Icons.Default.Warning
                                    EdgeType.OPPORTUNITY_LINK -> Icons.Default.Lightbulb
                                },
                                contentDescription = null,
                                tint = when (edge.edgeType) {
                                    EdgeType.DEMAND_FLOW -> Color(0xFF1565C0)
                                    EdgeType.EXTERNAL_LEAKAGE -> Color(0xFFC62828)
                                    EdgeType.OPPORTUNITY_LINK -> Color(0xFF7B1FA2)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${otherNode.name} (${otherNode.type.label})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = edge.volume,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClustersMatrixList(
    nodes: List<NetworkNode>,
    edges: List<NetworkEdge>,
    selectedNodeId: String?,
    onSelectNode: (String) -> Unit,
    onNavigateToOpportunity: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val clusters = nodes.groupBy { it.cluster }
        clusters.forEach { (clusterKey, clusterNodes) ->
            item {
                Text(
                    text = when (clusterKey) {
                        "TEXTILE" -> "Textiles & School Garments Cluster"
                        "HEALTHCARE" -> "Healthcare & Institutional Hygiene Cluster"
                        "FOOD" -> "Bakery & Food Processing Cluster"
                        else -> "$clusterKey Cluster"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DduPrimary
                )
            }

            items(clusterNodes) { node ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (node.id == selectedNodeId) node.type.containerColor else MaterialTheme.colorScheme.surface
                    ),
                    border = if (node.id == selectedNodeId) BorderStroke(1.5.dp, node.type.color) else null,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNode(node.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(node.type.containerColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (node.type) {
                                        NodeType.INSTITUTION -> Icons.Default.School
                                        NodeType.LOCAL_SHOP -> Icons.Default.Store
                                        NodeType.PRODUCT -> Icons.Default.Inventory2
                                        NodeType.EXTERNAL_SUPPLIER -> Icons.Default.LocalShipping
                                        NodeType.DDU_OPPORTUNITY -> Icons.Default.Lightbulb
                                    },
                                    contentDescription = null,
                                    tint = node.type.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = node.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "${node.type.label} • ${node.metrics}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (node.annualValue.isNotBlank()) {
                            Text(
                                text = node.annualValue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = node.type.color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GraphStatItem(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun GraphLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 10.sp, color = Color.White)
    }
}
