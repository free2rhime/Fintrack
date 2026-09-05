package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.ui.components.BadgeVariant
import com.example.ui.components.ButtonVariant
import com.example.ui.components.FinTrackButton
import com.example.ui.components.FinTrackCard
import com.example.ui.components.FinTrackEmptyState
import com.example.ui.components.FinTrackSegmentedControl
import com.example.ui.components.FinTrackStatusBadge
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseContainer
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeContainer
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusMedium
import com.example.ui.theme.RadiusXLarge
import com.example.ui.theme.SectionHeadline
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space32
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

    // Group categories by main category name
    val groupedCategories = remember(filteredCategories) {
        filteredCategories.groupBy { it.name }
    }

    Scaffold(
        containerColor = CanvasDark,
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (canManageCategories) {
                FloatingActionButton(
                    onClick = {
                        addDialogPreFilledCategory = ""
                        showAddDialog = true
                    },
                    containerColor = CobaltBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(RadiusLarge),
                    modifier = Modifier.testTag("fab_add_category")
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
                .background(CanvasDark),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = Space16)
            ) {
                Spacer(modifier = Modifier.height(Space16))

                // Header with tonal icon well and summary badge
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CobaltBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = CobaltBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Space12))
                        Column {
                            Text(
                                text = "Category & Subcategory Management",
                                style = SectionHeadline,
                                color = TextPrimary
                            )
                            Text(
                                text = "Organize household transactions by type",
                                style = MicroMetadata,
                                color = TextSecondary
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

                Spacer(modifier = Modifier.height(Space16))

                // Segmented Tab Toggle using FinTrackSegmentedControl
                FinTrackSegmentedControl(
                    items = listOf("Expense Categories", "Income Categories"),
                    selectedIndex = if (selectedType == "Expense") 0 else 1,
                    onItemSelected = { index ->
                        selectedType = if (index == 0) "Expense" else "Income"
                    }
                )

                Spacer(modifier = Modifier.height(Space16))

                // Categories List or Empty State
                if (groupedCategories.isEmpty()) {
                    FinTrackEmptyState(
                        icon = Icons.Default.Category,
                        iconTint = CobaltBlue,
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Space12)
                    ) {
                        items(groupedCategories.keys.toList(), key = { it }) { catName ->
                            val subList = groupedCategories[catName] ?: emptyList()
                            val isIncome = selectedType == "Income"
                            val iconVector = resolveCategoryIcon(catName, isIncome)
                            val iconBg = if (isIncome) IncomeContainer else ExpenseContainer
                            val iconTint = if (isIncome) IncomeEmerald else ExpenseCoral

                            FinTrackCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("category_card_${catName}"),
                                shape = RoundedCornerShape(RadiusLarge),
                                containerColor = SurfaceDark,
                                contentPadding = Space16
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // MAIN CATEGORY LINE (Header)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Tonal category icon container
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(iconBg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = null,
                                                    tint = iconTint,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(Space12))
                                            Column {
                                                Text(
                                                    text = catName,
                                                    style = CardTitleAmount,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                val validCount = subList.count { it.subCategory.isNotBlank() }
                                                Text(
                                                    text = if (validCount == 1) "1 subcategory" else "$validCount subcategories",
                                                    style = MicroMetadata,
                                                    color = TextSecondary
                                                )
                                            }
                                        }

                                        if (canManageCategories) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                // Add Subcategory Quick Action Button
                                                Surface(
                                                    shape = RoundedCornerShape(RadiusMedium),
                                                    color = CobaltBlue.copy(alpha = 0.12f),
                                                    modifier = Modifier
                                                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                                        .clip(RoundedCornerShape(RadiusMedium))
                                                        .clickable {
                                                            addDialogPreFilledCategory = catName
                                                            showAddDialog = true
                                                        }
                                                        .padding(horizontal = Space8, vertical = Space4)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = Space4)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Add Subcategory",
                                                            tint = CobaltBlue,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(Space4))
                                                        Text(
                                                            text = "+ Sub",
                                                            style = LabelBadgeMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = CobaltBlue
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(Space4))

                                                IconButton(
                                                    onClick = { categoryGroupToEdit = catName },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("edit_category_group_${catName}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Category Group",
                                                        tint = TextSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = { categoryGroupToDelete = catName },
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .testTag("delete_category_group_${catName}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Category Group",
                                                        tint = ExpenseCoral,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // SUBCATEGORIES LIST BELOW
                                    val validSubs = subList.filter { it.subCategory.isNotBlank() }
                                    if (validSubs.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(Space12))
                                        HorizontalDivider(
                                            color = SurfaceContainerHighDark.copy(alpha = 0.6f),
                                            thickness = 1.dp
                                        )
                                        Spacer(modifier = Modifier.height(Space12))

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(Space8)
                                        ) {
                                            validSubs.forEach { subEntity ->
                                                Surface(
                                                    shape = RoundedCornerShape(RadiusMedium),
                                                    color = SurfaceContainerDark,
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
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(SurfaceContainerHighDark),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Subtitles,
                                                                    contentDescription = null,
                                                                    tint = TextSecondary,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(Space8))
                                                            Text(
                                                                text = subEntity.subCategory,
                                                                style = BodyRegular,
                                                                color = TextPrimary,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                        }

                                                        if (canManageCategories) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                IconButton(
                                                                    onClick = { subcategoryToEdit = subEntity },
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("edit_subcategory_${subEntity.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Edit,
                                                                        contentDescription = "Edit Subcategory",
                                                                        tint = TextSecondary,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }

                                                                IconButton(
                                                                    onClick = { onDeleteSubcategory(subEntity.id) },
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("delete_subcategory_${subEntity.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = "Delete Subcategory",
                                                                        tint = ExpenseCoral,
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
                                        Text(
                                            text = if (canManageCategories) "No subcategories yet. Tap '+ Sub' to add one." else "No subcategories.",
                                            style = MicroMetadata,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(88.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CategoryFormDialog(
            title = if (addDialogPreFilledCategory.isNotBlank()) "Add Subcategory to ${addDialogPreFilledCategory}" else "Add New Category",
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

    // Category Group Edit Dialog
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

    // Category Group Delete Confirmation Dialog
    if (categoryGroupToDelete != null) {
        val groupToDelete = categoryGroupToDelete!!
        AlertDialog(
            onDismissRequest = { categoryGroupToDelete = null },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(RadiusXLarge),
            title = {
                Text(
                    text = "Delete Category Group",
                    style = SectionHeadline,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete category group '$groupToDelete'? All subcategories under this group will be deleted. Historical transactions will be preserved.",
                    style = BodyRegular,
                    color = TextSecondary
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

    // Subcategory Edit Dialog
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
            shape = RoundedCornerShape(RadiusXLarge),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Rename Category Group",
                    style = SectionHeadline,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Space16))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Category Group Name", color = TextSecondary) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Category group name cannot be empty", color = ExpenseCoral, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        errorBorderColor = ExpenseCoral,
                        errorLabelColor = ExpenseCoral,
                        cursorColor = CobaltBlue
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
            shape = RoundedCornerShape(RadiusXLarge),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .padding(Space20)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Rename Subcategory",
                    style = SectionHeadline,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Space16))
                OutlinedTextField(
                    value = subName,
                    onValueChange = {
                        subName = it
                        isError = false
                    },
                    label = { Text("Subcategory Name", color = TextSecondary) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Subcategory name cannot be empty", color = ExpenseCoral, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        errorBorderColor = ExpenseCoral,
                        errorLabelColor = ExpenseCoral,
                        cursorColor = CobaltBlue
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
            shape = RoundedCornerShape(RadiusXLarge),
            color = SurfaceDark
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
                    Text(
                        text = title,
                        style = SectionHeadline,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
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
                    label = { Text("Category Name (e.g. Food & Dining)", color = TextSecondary) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Category name cannot be empty", color = ExpenseCoral, style = MicroMetadata)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Category" },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        errorBorderColor = ExpenseCoral,
                        errorLabelColor = ExpenseCoral,
                        cursorColor = CobaltBlue
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
                    label = { Text("Subcategory (Optional)", color = TextSecondary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Subcategory" },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = SurfaceContainerDark,
                        unfocusedContainerColor = SurfaceContainerDark,
                        focusedBorderColor = CobaltBlue,
                        unfocusedBorderColor = SurfaceContainerHighDark,
                        cursorColor = CobaltBlue
                    )
                )

                Spacer(modifier = Modifier.height(Space12))

                // Helper Icon Picker (Tonal styling)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainerDark, RoundedCornerShape(RadiusMedium))
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
                            color = CobaltBlue
                        )

                        Row {
                            Text(
                                text = "Category",
                                style = MicroMetadata,
                                fontWeight = if (targetFieldForEmoji == "Category") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Category") CobaltBlue else TextMuted,
                                modifier = Modifier
                                    .clickable { targetFieldForEmoji = "Category" }
                                    .padding(horizontal = Space4)
                            )
                            Text("|", style = MicroMetadata, color = TextMuted)
                            Text(
                                text = "Subcategory",
                                style = MicroMetadata,
                                fontWeight = if (targetFieldForEmoji == "Subcategory") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Subcategory") CobaltBlue else TextMuted,
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
                                color = SurfaceContainerHighDark,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                                    .clip(CircleShape)
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

