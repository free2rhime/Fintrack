package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MaxContentWidthTablet
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.ShapeExtraLarge
import com.example.ui.theme.ShapeFloatingActionButton
import com.example.ui.theme.ShapeGroupedContainer
import com.example.ui.theme.ShapeGroupedItemBottom
import com.example.ui.theme.ShapeGroupedItemMiddle
import com.example.ui.theme.ShapeGroupedItemSingle
import com.example.ui.theme.ShapeGroupedItemTop
import com.example.ui.theme.ShapePill
import com.example.ui.theme.ShapeSquircleIcon
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space2
import com.example.ui.theme.Space20
import com.example.ui.theme.Space24
import com.example.ui.theme.Space32
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress

/**
 * Material 3 Expressive Category Gallery Screen — FinTrack WOW Edition.
 *
 * Implements Checkpoint 7 & M3-7 visual and interaction modernization:
 * - Expressive Category Gallery with grouped tonal surface containers (`ShapeGroupedContainer`)
 * - Responsive Wrap Categories Architecture: dynamic layout reflow across 360dp–720dp+ eliminating horizontal clipping, text truncation, and action overflow
 * - Smooth spring-driven accordion expansion physics with reduced-motion accessibility
 * - Tactile press feedback (`tactilePress()`) across all interactive cards, pill buttons, and controls
 * - Strict RBAC enforcement: OWNER/Admin management controls vs clean MEMBER read-only presentation
 * - Accessible >=48dp interactive touch targets and semantic role definitions
 */
@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    onAddCategory: (name: String, type: String, subCategory: String) -> Unit,
    onUpdateCategoryGroup: (oldName: String, newName: String, type: String) -> Unit,
    onDeleteCategoryGroup: (name: String, type: String) -> Unit,
    onUpdateSubcategory: (id: String, newSubCategory: String) -> Unit,
    onDeleteSubcategory: (id: String) -> Unit,
    canManageCategories: Boolean = true,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf("Expense") }
    var showAddDialog by remember { mutableStateOf(false) }
    var addDialogPreFilledCategory by remember { mutableStateOf("") }

    var categoryGroupToEdit by remember { mutableStateOf<String?>(null) }
    var categoryGroupToDelete by remember { mutableStateOf<String?>(null) }
    var subcategoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }

    // Tracks collapsed state for each category group.
    // Default is expanded (isExpanded = !(collapsedCategories[catName] == true))
    // to preserve immediate visibility in automated UI tests and screen readers.
    val collapsedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val reducedMotion = isReducedMotionEnabled()

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Group categories by main category name
    val groupedCategories = remember(filteredCategories) {
        filteredCategories.groupBy { it.name }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (canManageCategories) {
                FloatingActionButton(
                    onClick = {
                        addDialogPreFilledCategory = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = ShapeFloatingActionButton,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                        .tactilePress()
                        .testTag("fab_add_category")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = MaxContentWidthTablet)
                    .padding(horizontal = Space16)
            ) {
                Spacer(modifier = Modifier.height(Space8))

                // Expressive Header Hero with tonal squircle icon badge and responsive group count
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isNarrow = maxWidth < 260.dp
                    if (isNarrow) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(Space8)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(ShapeSquircleIcon)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space12))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Categories & Subcategories",
                                        style = SectionHeadline,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = true,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        text = "Organize household transactions by type",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                            }
                            if (groupedCategories.isNotEmpty()) {
                                FinTrackStatusBadge(
                                    label = "${groupedCategories.size} ${if (groupedCategories.size == 1) "Group" else "Groups"}",
                                    variant = BadgeVariant.NEUTRAL
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(ShapeSquircleIcon)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(Space12))
                                Column {
                                    Text(
                                        text = "Categories & Subcategories",
                                        style = SectionHeadline,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        softWrap = true,
                                        modifier = Modifier.semantics { heading() }
                                    )
                                    Text(
                                        text = "Organize household transactions by type",
                                        style = MicroMetadata,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = true
                                    )
                                }
                            }

                            if (groupedCategories.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(Space8))
                                FinTrackStatusBadge(
                                    label = "${groupedCategories.size} ${if (groupedCategories.size == 1) "Group" else "Groups"}",
                                    variant = BadgeVariant.NEUTRAL
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space8))

                // Segmented Tab Toggle with M3 Expressive Spring Physics
                FinTrackSegmentedControl(
                    items = listOf("Expense Categories", "Income Categories"),
                    selectedIndex = if (selectedType == "Expense") 0 else 1,
                    onItemSelected = { index ->
                        selectedType = if (index == 0) "Expense" else "Income"
                    }
                )

                Spacer(modifier = Modifier.height(Space8))

                // Category Gallery List or Empty State
                if (groupedCategories.isEmpty()) {
                    FinTrackEmptyState(
                        icon = Icons.Default.Category,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = if (selectedType == "Expense") "No Expense Categories" else "No Income Categories",
                        description = if (canManageCategories) {
                            "Create category groups and subcategories to organize your transactions."
                        } else {
                            "No categories available in this household."
                        },
                        actionLabel = if (canManageCategories) "Add Category" else null,
                        onActionClick = if (canManageCategories) {
                            {
                                addDialogPreFilledCategory = ""
                                showAddDialog = true
                            }
                        } else null,
                        modifier = Modifier.padding(top = Space32)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .testTag("categories_list"),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        groupedCategories.keys.forEach { catName ->
                            val subList = groupedCategories[catName] ?: emptyList()
                            val isIncome = selectedType == "Income"
                            val iconVector = resolveCategoryIcon(catName, isIncome)
                            val iconBg = if (isIncome) FinTrackTheme.colors.incomeContainer else FinTrackTheme.colors.expenseContainer
                            val iconTint = if (isIncome) FinTrackTheme.colors.income else FinTrackTheme.colors.expense
                            val isExpanded = collapsedCategories[catName] != true

                            val cardBgColor by animateColorAsState(
                                targetValue = if (isExpanded) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.contentSpring(),
                                label = "cardBgColor_$catName"
                            )

                            val chevronTint by animateColorAsState(
                                targetValue = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                label = "chevronTint_$catName"
                            )

                            val chevronRotation by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                animationSpec = if (reducedMotion) snap() else FinTrackMotion.interactiveSpring(),
                                label = "chevronRotation_$catName"
                            )

                            Surface(
                                shape = ShapeGroupedContainer,
                                color = cardBgColor,
                                tonalElevation = if (isExpanded) 1.dp else 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_card_${catName}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Space16, vertical = Space12)
                                ) {
                                    // CATEGORY HEADER (Responsive & Wrap-Proof Layout)
                                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                        val containerWidth = maxWidth
                                        val isWideHeader = containerWidth >= 520.dp

                                        if (canManageCategories && !isWideHeader) {
                                            // RESPONSIVE TWO-TIER COMPOSITION FOR COMPACT & MEDIUM MOBILE VIEWPORTS (< 520dp)
                                            // Zone 1: Category Identity & Expand Chevron
                                            // Zone 2: Tactile Management Toolbar (+ Sub, Edit, Delete)
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(RadiusMedium))
                                                        .clickable(
                                                            role = Role.Button,
                                                            onClick = { collapsedCategories[catName] = isExpanded }
                                                        )
                                                        .padding(vertical = Space4),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Left: Icon + Title & Subcategory Count Badge
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(end = Space8)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(44.dp)
                                                                .clip(ShapeSquircleIcon)
                                                                .background(iconBg),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = iconVector,
                                                                contentDescription = null,
                                                                tint = iconTint,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(Space12))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = catName,
                                                                style = CardTitleAmount,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface,
                                                                maxLines = 2,
                                                                overflow = TextOverflow.Ellipsis,
                                                                softWrap = true
                                                            )
                                                            Spacer(modifier = Modifier.height(Space4))
                                                            val validCount = subList.count { it.subCategory.isNotBlank() }
                                                            Surface(
                                                                shape = ShapePill,
                                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                                modifier = Modifier.clip(ShapePill)
                                                            ) {
                                                                Text(
                                                                    text = if (validCount == 1) "1 subcategory" else "$validCount subcategories",
                                                                    style = MicroMetadata,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    maxLines = 1,
                                                                    softWrap = false,
                                                                    modifier = Modifier.padding(horizontal = Space8, vertical = Space2)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Chevron Expand / Collapse Trigger
                                                    IconButton(
                                                        onClick = { collapsedCategories[catName] = isExpanded },
                                                        modifier = Modifier
                                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                            .size(48.dp)
                                                            .tactilePress()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = if (isExpanded) "Collapse $catName" else "Expand $catName",
                                                            tint = chevronTint,
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .rotate(chevronRotation)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(Space4))

                                                // Zone 2: Tactile Management Action Toolbar
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = Space2),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Quick Action Pill "+ Sub" with Tactile Press
                                                    Surface(
                                                        shape = ShapePill,
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        modifier = Modifier
                                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                            .clip(ShapePill)
                                                            .tactilePress()
                                                            .clickable(
                                                                role = Role.Button,
                                                                onClick = {
                                                                    addDialogPreFilledCategory = catName
                                                                    showAddDialog = true
                                                                }
                                                            )
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Add,
                                                                contentDescription = "Add Subcategory",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(Space4))
                                                            Text(
                                                                text = "+ Sub",
                                                                style = LabelBadgeMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }

                                                    // Management Action Cluster: Edit & Delete
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(Space4)
                                                    ) {
                                                        IconButton(
                                                            onClick = { categoryGroupToEdit = catName },
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                .size(48.dp)
                                                                .tactilePress()
                                                                .testTag("edit_category_group_${catName}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Category Group",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { categoryGroupToDelete = catName },
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                .size(48.dp)
                                                                .tactilePress()
                                                                .testTag("delete_category_group_${catName}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Category Group",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            // SINGLE BALANCED ROW (For Wide Viewports >= 520dp OR Member Read-Only Mode)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(RadiusMedium))
                                                    .clickable(
                                                        role = Role.Button,
                                                        onClick = { collapsedCategories[catName] = isExpanded }
                                                    )
                                                    .padding(vertical = Space4),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Left: Category Icon + Title + Subcount
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = if (canManageCategories) Space16 else Space8)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(44.dp)
                                                            .clip(ShapeSquircleIcon)
                                                            .background(iconBg),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = iconVector,
                                                            contentDescription = null,
                                                            tint = iconTint,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(Space12))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = catName,
                                                            style = CardTitleAmount,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            softWrap = true
                                                        )
                                                        Spacer(modifier = Modifier.height(Space4))
                                                        val validCount = subList.count { it.subCategory.isNotBlank() }
                                                        Surface(
                                                            shape = ShapePill,
                                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                            modifier = Modifier.clip(ShapePill)
                                                        ) {
                                                            Text(
                                                                text = if (validCount == 1) "1 subcategory" else "$validCount subcategories",
                                                                style = MicroMetadata,
                                                                fontWeight = FontWeight.Medium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = Space8, vertical = Space2)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Right: Management Actions & Accordion Trigger
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    if (canManageCategories) {
                                                        // Quick Action Pill "+ Sub" with Tactile Press
                                                        Surface(
                                                            shape = ShapePill,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                .clip(ShapePill)
                                                                .tactilePress()
                                                                .clickable(
                                                                    role = Role.Button,
                                                                    onClick = {
                                                                        addDialogPreFilledCategory = catName
                                                                        showAddDialog = true
                                                                    }
                                                                )
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Add,
                                                                    contentDescription = "Add Subcategory",
                                                                    tint = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(modifier = Modifier.width(Space4))
                                                                Text(
                                                                    text = "+ Sub",
                                                                    style = LabelBadgeMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(Space4))

                                                        IconButton(
                                                            onClick = { categoryGroupToEdit = catName },
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                .size(48.dp)
                                                                .tactilePress()
                                                                .testTag("edit_category_group_${catName}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Category Group",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { categoryGroupToDelete = catName },
                                                            modifier = Modifier
                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                .size(48.dp)
                                                                .tactilePress()
                                                                .testTag("delete_category_group_${catName}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Category Group",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    // Chevron Expand / Collapse Trigger
                                                    IconButton(
                                                        onClick = { collapsedCategories[catName] = isExpanded },
                                                        modifier = Modifier
                                                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                            .size(48.dp)
                                                            .tactilePress()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = if (isExpanded) "Collapse $catName" else "Expand $catName",
                                                            tint = chevronTint,
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .rotate(chevronRotation)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // SUBCATEGORIES ACCORDION EXPANSION
                                    AnimatedVisibility(
                                        visible = isExpanded,
                                        enter = if (reducedMotion) fadeIn(animationSpec = snap()) else expandVertically(
                                            animationSpec = FinTrackMotion.contentSpring()
                                        ) + fadeIn(animationSpec = FinTrackMotion.contentSpring()),
                                        exit = if (reducedMotion) fadeOut(animationSpec = snap()) else shrinkVertically(
                                            animationSpec = FinTrackMotion.contentSpring()
                                        ) + fadeOut(animationSpec = FinTrackMotion.contentSpring())
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            val validSubs = subList.filter { it.subCategory.isNotBlank() }
                                            if (validSubs.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(Space8))
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    thickness = 1.dp
                                                )
                                                Spacer(modifier = Modifier.height(Space8))

                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(Space8)
                                                ) {
                                                    validSubs.forEachIndexed { index, subEntity ->
                                                        val shape = when {
                                                            validSubs.size == 1 -> ShapeGroupedItemSingle
                                                            index == 0 -> ShapeGroupedItemTop
                                                            index == validSubs.size - 1 -> ShapeGroupedItemBottom
                                                            else -> ShapeGroupedItemMiddle
                                                        }

                                                        Surface(
                                                            shape = shape,
                                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = Space12, vertical = Space8),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .padding(end = Space8)
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .clip(ShapeSquircleIcon)
                                                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Subtitles,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(Space8))
                                                                    Text(
                                                                        text = subEntity.subCategory,
                                                                        style = BodyRegular,
                                                                        color = MaterialTheme.colorScheme.onSurface,
                                                                        fontWeight = FontWeight.Medium,
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        softWrap = true
                                                                    )
                                                                }

                                                                if (canManageCategories) {
                                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        IconButton(
                                                                            onClick = { subcategoryToEdit = subEntity },
                                                                            modifier = Modifier
                                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                                .size(48.dp)
                                                                                .tactilePress()
                                                                                .testTag("edit_subcategory_${subEntity.id}")
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Edit,
                                                                                contentDescription = "Edit Subcategory",
                                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                        }

                                                                        IconButton(
                                                                            onClick = { onDeleteSubcategory(subEntity.id) },
                                                                            modifier = Modifier
                                                                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                                                .size(48.dp)
                                                                                .tactilePress()
                                                                                .testTag("delete_subcategory_${subEntity.id}")
                                                                        ) {
                                                                            Icon(
                                                                                imageVector = Icons.Default.Delete,
                                                                                contentDescription = "Delete Subcategory",
                                                                                tint = MaterialTheme.colorScheme.error,
                                                                                modifier = Modifier.size(16.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.height(Space8))
                                                Surface(
                                                    shape = ShapeGroupedItemSingle,
                                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = Space12, vertical = Space8),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(ShapeSquircleIcon)
                                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Category,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(Space8))
                                                        Text(
                                                            text = if (canManageCategories) "No subcategories yet. Tap '+ Sub' to add one." else "No subcategories.",
                                                            style = MicroMetadata,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(88.dp))
                    }
                }
            }
        }
    }

    // ADD CATEGORY / SUBCATEGORY DIALOG
    if (showAddDialog) {
        CategoryFormDialog(
            title = if (addDialogPreFilledCategory.isNotBlank()) "Add Subcategory to $addDialogPreFilledCategory" else "Add New Category",
            initialName = addDialogPreFilledCategory,
            initialSubCategory = "",
            defaultType = selectedType,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, subCategory ->
                onAddCategory(name, type, subCategory)
                showAddDialog = false
            }
        )
    }

    // CATEGORY GROUP EDIT DIALOG
    if (categoryGroupToEdit != null) {
        val oldGroup = categoryGroupToEdit!!
        CategoryHeaderEditDialog(
            oldCategoryName = oldGroup,
            onDismiss = { categoryGroupToEdit = null },
            onConfirm = { newName ->
                onUpdateCategoryGroup(oldGroup, newName, selectedType)
                categoryGroupToEdit = null
            }
        )
    }

    // CATEGORY GROUP DELETE CONFIRMATION DIALOG
    if (categoryGroupToDelete != null) {
        val groupToDelete = categoryGroupToDelete!!
        AlertDialog(
            onDismissRequest = { categoryGroupToDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = ShapeExtraLarge,
            title = {
                Text(
                    text = "Delete Category Group",
                    style = SectionHeadline,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete category group '$groupToDelete'? All subcategories under this group will be deleted. Historical transactions will be preserved.",
                    style = BodyRegular,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                FinTrackButton(
                    text = "Delete Group",
                    onClick = {
                        onDeleteCategoryGroup(groupToDelete, selectedType)
                        categoryGroupToDelete = null
                    },
                    variant = ButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                FinTrackButton(
                    text = "Cancel",
                    onClick = { categoryGroupToDelete = null },
                    variant = ButtonVariant.SECONDARY
                )
            }
        )
    }

    // SUBCATEGORY EDIT DIALOG
    if (subcategoryToEdit != null) {
        val subEntity = subcategoryToEdit!!
        SubcategoryEditDialog(
            initialSubCategory = subEntity.subCategory,
            onDismiss = { subcategoryToEdit = null },
            onConfirm = { newSubName ->
                onUpdateSubcategory(subEntity.id, newSubName)
                subcategoryToEdit = null
            }
        )
    }
}

@Composable
private fun CategoryHeaderEditDialog(
    oldCategoryName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var name by remember { mutableStateOf(oldCategoryName) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(Space16),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(ShapeSquircleIcon)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space12))
                    Text(
                        text = "Rename Category Group",
                        style = SectionHeadline,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(Space16))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Category Group Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Category group name cannot be empty", color = MaterialTheme.colorScheme.error, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(Space20))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinTrackButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.width(Space8))
                    FinTrackButton(
                        text = "Save",
                        onClick = {
                            if (name.isBlank()) {
                                isError = true
                                return@FinTrackButton
                            }
                            onConfirm(name.trim())
                        },
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryEditDialog(
    initialSubCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (newSubCategory: String) -> Unit
) {
    var subName by remember { mutableStateOf(initialSubCategory) }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(Space16),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(ShapeSquircleIcon)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Space12))
                    Text(
                        text = "Rename Subcategory",
                        style = SectionHeadline,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(Space16))
                OutlinedTextField(
                    value = subName,
                    onValueChange = {
                        subName = it
                        isError = false
                    },
                    label = { Text("Subcategory Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Subcategory name cannot be empty", color = MaterialTheme.colorScheme.error, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(Space20))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinTrackButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.width(Space8))
                    FinTrackButton(
                        text = "Save",
                        onClick = {
                            if (subName.isBlank()) {
                                isError = true
                                return@FinTrackButton
                            }
                            onConfirm(subName.trim())
                        },
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryFormDialog(
    title: String,
    initialName: String,
    initialSubCategory: String,
    defaultType: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, subCategory: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var subCategory by remember { mutableStateOf(initialSubCategory) }
    var type by remember { mutableStateOf(defaultType) }
    var isError by remember { mutableStateOf(false) }

    // Preset emoji icons as helper shortcuts
    val commonEmojis = listOf(
        "💸", "💹", "💼", "🍉", "🍔", "🛒", "🏠", "⚡", "🚗", "⛽", "🛍️", "👕",
        "🏥", "💊", "🏋️", "🎬", "🍿", "✈️", "🏦", "💳", "🎁", "💻", "📈", "📊",
        "💵", "🎉", "🍕", "🥗", "🎮", "🐾", "🛠️", "💡", "🏷️", "💰", "💎", "🚌", "🔑"
    )

    var targetFieldForEmoji by remember { mutableStateOf("Category") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(vertical = Space8, horizontal = Space16),
            shape = ShapeExtraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(ShapeSquircleIcon)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Text(
                            text = title,
                            style = SectionHeadline,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            softWrap = true
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                            .tactilePress()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Space16))

                // Type selector
                FinTrackSegmentedControl(
                    items = listOf("Expense", "Income"),
                    selectedIndex = if (type == "Expense") 0 else 1,
                    onItemSelected = { index ->
                        type = if (index == 0) "Expense" else "Income"
                    }
                )

                Spacer(modifier = Modifier.height(Space16))

                // Category Text Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                        targetFieldForEmoji = "Category"
                    },
                    label = { Text("Category Name (e.g. Food & Dining)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Category name cannot be empty", color = MaterialTheme.colorScheme.error, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Category" },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(Space12))

                // Subcategory Text Field
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = {
                        subCategory = it
                        targetFieldForEmoji = "Subcategory"
                    },
                    label = { Text("Subcategory (Optional)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Subcategory" },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(Space12))

                // Helper Icon Picker (Tonal styling)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(RadiusMedium))
                        .padding(Space12)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Prefix to $targetFieldForEmoji:",
                            style = MicroMetadata,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row {
                            Text(
                                text = "Category",
                                style = MicroMetadata,
                                fontWeight = if (targetFieldForEmoji == "Category") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Category") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { targetFieldForEmoji = "Category" }
                                    .padding(horizontal = Space4)
                            )
                            Text("|", style = MicroMetadata, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Subcategory",
                                style = MicroMetadata,
                                fontWeight = if (targetFieldForEmoji == "Subcategory") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Subcategory") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { targetFieldForEmoji = "Subcategory" }
                                    .padding(horizontal = Space4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Space8))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Space4)
                    ) {
                        commonEmojis.forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                    .clip(CircleShape)
                                    .tactilePress()
                                    .clickable {
                                        if (targetFieldForEmoji == "Category") {
                                            name = if (name.startsWith(emoji)) name else "$emoji $name".trim()
                                        } else {
                                            subCategory = if (subCategory.startsWith(emoji)) subCategory else "$emoji $subCategory".trim()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Space20))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinTrackButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.width(Space8))
                    FinTrackButton(
                        text = "Save Category",
                        onClick = {
                            if (name.isBlank()) {
                                isError = true
                                return@FinTrackButton
                            }
                            onConfirm(name.trim(), type, subCategory.trim())
                        },
                        variant = ButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
