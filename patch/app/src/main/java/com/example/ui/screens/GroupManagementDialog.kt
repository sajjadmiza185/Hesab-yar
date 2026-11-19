package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.Customer
import com.example.model.CustomerGroup

private val groupColors = listOf(
    "#2196F3",
    "#4CAF50",
    "#FF9800",
    "#9C27B0",
    "#F44336",
    "#009688",
    "#795548",
    "#607D8B"
)

@Composable
fun GroupManagementDialog(
    groups: List<CustomerGroup>,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSaveGroup: (
        id: Long,
        name: String,
        description: String,
        colorHex: String,
        leaderCustomerId: Long?
    ) -> Unit,
    onDeleteGroup: (CustomerGroup) -> Unit,
    onAssignCustomers: (CustomerGroup, List<Long>) -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<CustomerGroup?>(null) }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var deletingGroup by remember { mutableStateOf<CustomerGroup?>(null) }

    var groupForMembers by remember { mutableStateOf<CustomerGroup?>(null) }

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onDismissRequest = onDismiss,
        title = {
            Text("مدیریت گروه‌های مشتریان")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        editingGroup = null
                        showEditor = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Text(
                        text = " افزودن گروه جدید",
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                if (groups.isEmpty()) {
                    Text(
                        text = "هنوز گروهی ایجاد نشده است.",
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = groups,
                            key = { it.id }
                        ) { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant
                                )
                            ,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp)
    ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val groupColor = remember(group.colorHex) {
                                            try {
                                                Color(
                                                    android.graphics.Color.parseColor(
                                                        group.colorHex
                                                    )
                                                )
                                            } catch (_: Exception) {
                                                Color.Gray
                                            }
                                        }

                                        Spacer(
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment =
                                                    Alignment.CenterVertically
                                            ) {
                                                Spacer(
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                )

                                                Text(
                                                    text = group.name,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }

                                            if (group.description.isNotBlank()) {
                                                Text(
                                                    text = group.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(
                                                verticalAlignment =
                                                    Alignment.CenterVertically
                                            ) {
                                                Spacer(
                                                    modifier = Modifier
                                                        .padding(end = 6.dp)
                                                )
                                                androidx.compose.foundation.layout.Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = groupColor,
                                                            shape = CircleShape
                                                        )
                                                        .padding(5.dp)
                                                )
                                                Text(
                                                    text = " ${customers.count {
                                                        it.groupId == group.id
                                                    }} مشتری",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                editingGroup = group
                                                showEditor = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "ویرایش گروه"
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                deletingGroup = group
                                                showDeleteConfirmation = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف گروه",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            groupForMembers = group
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null
                                        )
                                        Text(
                                            text = " انتخاب و مدیریت اعضای گروه",
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )

    if (showEditor) {
        GroupEditorDialog(
            group = editingGroup,
            onDismiss = {
                showEditor = false
                editingGroup = null
            },
            customers = customers,
            onSave = { id, name, description, color, leaderCustomerId ->
                onSaveGroup(id, name, description, color, leaderCustomerId)
                showEditor = false
                editingGroup = null
            }
        )
    }

    if (showDeleteConfirmation) {
        val group = deletingGroup

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = {
                showDeleteConfirmation = false
                deletingGroup = null
            },
            title = {
                Text("حذف گروه")
            },
            text = {
                Text(
                    "آیا از حذف گروه «${group?.name.orEmpty()}» مطمئن هستید؟"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        group?.let(onDeleteGroup)
                        showDeleteConfirmation = false
                        deletingGroup = null
                    }
                ) {
                    Text(
                        "حذف",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        deletingGroup = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }

    groupForMembers?.let { selectedGroup ->
        var selectedCustomerIds by remember(selectedGroup.id, customers) {
            mutableStateOf(
                customers
                    .filter { it.groupId == selectedGroup.id }
                    .map { it.id }
                    .toSet()
            )
        }

        AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
            onDismissRequest = {
                groupForMembers = null
            },
            title = {
                Text("اعضای گروه ${selectedGroup.name}")
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "مشتریان موردنظر را انتخاب کن. " +
                            "مشتریانی که از انتخاب خارج شوند، از این گروه حذف می‌شوند.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (customers.isEmpty()) {
                        Text("هنوز مشتری‌ای ثبت نشده است.")
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                        ) {
                            items(
                                items = customers,
                                key = { it.id }
                            ) { customer ->
                                val isSelected =
                                    customer.id in selectedCustomerIds

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCustomerIds =
                                                if (isSelected) {
                                                    selectedCustomerIds -
                                                        customer.id
                                                } else {
                                                    selectedCustomerIds +
                                                        customer.id
                                                }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedCustomerIds =
                                                if (checked) {
                                                    selectedCustomerIds +
                                                        customer.id
                                                } else {
                                                    selectedCustomerIds -
                                                        customer.id
                                                }
                                        }
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(customer.name)

                                        Text(
                                            text = customer.phone,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (
                                            customer.groupId != null &&
                                            customer.groupId != selectedGroup.id
                                        ) {
                                            Text(
                                                text = "با ذخیره، به این گروه منتقل می‌شود",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAssignCustomers(
                            selectedGroup,
                            selectedCustomerIds.toList()
                        )
                        groupForMembers = null
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null
                    )
                    Text(
                        text = " ذخیره اعضا",
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        groupForMembers = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun GroupEditorDialog(
    group: CustomerGroup?,
    customers: List<Customer>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, Long?) -> Unit
) {
    val groupMembers = remember(group?.id, customers) {
        if (group == null) emptyList() else customers.filter { it.groupId == group.id }
    }
    var selectedLeaderId by remember(group?.id, group?.leaderCustomerId) {
        mutableStateOf(group?.leaderCustomerId?.takeIf { id -> groupMembers.any { it.id == id } })
    }
    var showLeaderPicker by remember { mutableStateOf(false) }
    var name by remember(group?.id) {
        mutableStateOf(group?.name.orEmpty())
    }

    var description by remember(group?.id) {
        mutableStateOf(group?.description.orEmpty())
    }

    var selectedColor by remember(group?.id) {
        mutableStateOf(
            group?.colorHex?.takeIf { it.isNotBlank() }
                ?: groupColors.first()
        )
    }

    val nameIsValid = name.trim().isNotEmpty()

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (group == null) {
                    "افزودن گروه جدید"
                } else {
                    "ویرایش گروه"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام گروه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "سرگروه",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(
                    onClick = { showLeaderPicker = true },
                    enabled = groupMembers.isNotEmpty()
                ) {
                    val leaderName = groupMembers.firstOrNull { it.id == selectedLeaderId }?.name
                    Text(leaderName ?: if (groupMembers.isEmpty()) "ابتدا اعضای گروه را تعیین کنید" else "انتخاب سرگروه")
                }

                Text(
                    text = "رنگ گروه",
                    style = MaterialTheme.typography.titleSmall
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groupColors) { colorHex ->
                        val color = remember(colorHex) {
                            Color(
                                android.graphics.Color.parseColor(colorHex)
                            )
                        }

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .background(
                                    color = color,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = colorHex
                                }
                                .padding(12.dp)
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "رنگ انتخاب‌شده",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameIsValid,
                onClick = {
                    onSave(
                        group?.id ?: 0L,
                        name.trim(),
                        description.trim(),
                        selectedColor,
                        selectedLeaderId
                    )
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )

    if (showLeaderPicker) {
        AlertDialog(
            onDismissRequest = { showLeaderPicker = false },
            title = { Text("انتخاب سرگروه") },
            text = {
                LazyColumn {
                    items(groupMembers) { member ->
                        TextButton(
                            onClick = {
                                selectedLeaderId = member.id
                                showLeaderPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (member.id == selectedLeaderId) "✓ ${member.name}" else member.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedLeaderId = null
                    showLeaderPicker = false
                }) { Text("بدون سرگروه") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaderPicker = false }) { Text("بستن") }
            }
        )
    }
}

// انتخاب سرگروه فقط از میان اعضای فعلی همان گروه انجام می‌شود.
