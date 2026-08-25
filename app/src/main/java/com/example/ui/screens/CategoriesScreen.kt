package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen

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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            if (canManageCategories) {
                FloatingActionButton(
                    onClick = {
                        addDialogPreFilledCategory = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_category")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Category & Subcategory Management",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented Tab Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == "Expense",
                    onClick = { selectedType = "Expense" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(
                        "Expense Categories",
                        color = if (selectedType == "Expense") ExpenseRed else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                SegmentedButton(
                    selected = selectedType == "Income",
                    onClick = { selectedType = "Income" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(
                        "Income Categories",
                        color = if (selectedType == "Income") IncomeGreen else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Categories Grouped List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(groupedCategories.keys.toList(), key = { it }) { catName ->
                    val subList = groupedCategories[catName] ?: emptyList()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_card_${catName}"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
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
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    )
                                }

                                if (canManageCategories) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Add Subcategory Quick Action Button
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    addDialogPreFilledCategory = catName
                                                    showAddDialog = true
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Subcategory",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "+ Sub",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        IconButton(
                                            onClick = { categoryGroupToEdit = catName },
                                            modifier = Modifier.size(32.dp).testTag("edit_category_group_${catName}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Category Group",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { categoryGroupToDelete = catName },
                                            modifier = Modifier.size(32.dp).testTag("delete_category_group_${catName}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Category Group",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // SUBCATEGORIES LIST BELOW
                            val validSubs = subList.filter { it.subCategory.isNotBlank() }
                            if (validSubs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    validSubs.forEach { subEntity ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Subtitles,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = subEntity.subCategory,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }

                                                if (canManageCategories) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = { subcategoryToEdit = subEntity },
                                                            modifier = Modifier.size(28.dp).testTag("edit_subcategory_${subEntity.id}")
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
                                                            modifier = Modifier.size(28.dp).testTag("delete_subcategory_${subEntity.id}")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete Subcategory",
                                                                tint = ExpenseRed,
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
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (canManageCategories) "No subcategories yet. Tap '+ Sub' to add one." else "No subcategories.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
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
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { categoryGroupToDelete = null },
            title = { Text("Delete Category Group") },
            text = { Text("Are you sure you want to delete category group '$groupToDelete'? All subcategories under this group will be deleted. Historical transactions will be preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategoryGroup(groupToDelete, selectedType)
                        categoryGroupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Delete Group", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { categoryGroupToDelete = null }) {
                    Text("Cancel")
                }
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
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Rename Category Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Category Group Name") },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                isError = true
                                return@Button
                            }
                            onConfirm(name.trim())
                        }
                    ) {
                        Text("Save")
                    }
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
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Rename Subcategory",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = subName,
                    onValueChange = {
                        subName = it
                        isError = false
                    },
                    label = { Text("Subcategory Name") },
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subName.isBlank()) {
                                isError = true
                                return@Button
                            }
                            onConfirm(subName.trim())
                        }
                    ) {
                        Text("Save")
                    }
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

    // Financial & Lifestyle Emoji Preset Picker
    val commonEmojis = listOf(
        "💸", "💹", "💼", "🍉", "🍔", "🛒", "🏠", "⚡", "🚗", "⛽", "🛍️", "👕",
        "🏥", "💊", "🏋️", "🎬", "🍿", "✈️", "🏦", "💳", "🎁", "💻", "📈", "📊",
        "💵", "🎉", "🍕", "🥗", "🎮", "🐾", "🛠️", "💡", "🏷️", "💰", "💎", "🚌", "🔑"
    )

    var targetFieldForEmoji by remember { mutableStateOf("Category") } // "Category" or "Subcategory"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == "Expense",
                        onClick = { type = "Expense" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Expense")
                    }

                    SegmentedButton(
                        selected = type == "Income",
                        onClick = { type = "Income" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Income")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Text Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                        targetFieldForEmoji = "Category"
                    },
                    label = { Text("Category Name (e.g. 💸 Financial & Taxes)") },
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Category" },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Subcategory Text Field
                OutlinedTextField(
                    value = subCategory,
                    onValueChange = {
                        subCategory = it
                        targetFieldForEmoji = "Subcategory"
                    },
                    label = { Text("Subcategory (e.g. 💹 Investments & Dividends)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { targetFieldForEmoji = "Subcategory" },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // EMOJI CHIP SELECTOR LIST
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap Emoji for ${targetFieldForEmoji}:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (targetFieldForEmoji == "Category") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Category") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { targetFieldForEmoji = "Category" }
                                    .padding(horizontal = 4.dp)
                            )
                            Text("|", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "Subcategory",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (targetFieldForEmoji == "Subcategory") FontWeight.Bold else FontWeight.Normal,
                                color = if (targetFieldForEmoji == "Subcategory") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { targetFieldForEmoji = "Subcategory" }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonEmojis.forEach { emoji ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 1.dp,
                                modifier = Modifier
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            isError = true
                            return@Button
                        }
                        onConfirm(name.trim(), type, subCategory.trim())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Category", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
