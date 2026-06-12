package com.cjlu.studentapp.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cjlu.studentapp.R
import com.cjlu.studentapp.network.api.MessageDto
import kotlinx.coroutines.launch

private data class InboxMessageUi(
    val id: String,
    val sender: String,
    val title: String,
    val body: String,
    val timeLabel: String,
    val category: MessageCategory,
    val relatedServiceId: String?,
    val requiresAction: Boolean,
    val isRead: Boolean,
)

private fun MessageDto.toUi(): InboxMessageUi =
    InboxMessageUi(
        id = id,
        sender = sender,
        title = title,
        body = body,
        timeLabel = timeLabel,
        category = runCatching { MessageCategory.valueOf(category) }
            .getOrElse { MessageCategory.Announcements },
        relatedServiceId = relatedServiceId,
        requiresAction = requiresAction,
        isRead = isRead,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    messages: List<MessageDto>,
    onReloadMessages: suspend () -> Unit,
    onSetMessageRead: suspend (String, Boolean) -> Unit,
    isBackendAvailable: Boolean = true,
    onServiceSelected: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uiMessages = remember(messages) { messages.map { it.toUi() } }
    val filterItems = remember { messageFilterItems() }
    var selectedFilterKey by rememberSaveable { mutableStateOf(MessageFilterKey.ALL) }
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }

    val unreadCount = uiMessages.count { !it.isRead }
    val actionCount = uiMessages.count { it.requiresAction }
    val selectedMessage = selectedMessageId?.let { id -> uiMessages.firstOrNull { it.id == id } }

    val filteredMessages = uiMessages.filter { message ->
        when (selectedFilterKey) {
            MessageFilterKey.ALL -> true
            MessageFilterKey.UNREAD -> !message.isRead
            else -> message.category.name == selectedFilterKey
        }
    }

    if (selectedMessage != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { selectedMessageId = null },
            sheetState = sheetState
        ) {
            MessageDetailSheet(
                message = selectedMessage,
                onClose = { selectedMessageId = null },
                onToggleRead = {
                    scope.launch {
                        onSetMessageRead(selectedMessage.id, !selectedMessage.isRead)
                        onReloadMessages()
                    }
                },
            ) {
                selectedMessage.relatedServiceId?.let { serviceId ->
                    selectedMessageId = null
                    onServiceSelected(serviceId)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.messages_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.messages_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            MessagesOverviewRow(
                totalMessages = uiMessages.size,
                unreadMessages = unreadCount,
                actionMessages = actionCount
            )
        }

        if (!isBackendAvailable) {
            item {
                OfflineNoticeCard(
                    title = stringResource(R.string.common_offline_mode),
                    message = stringResource(R.string.messages_empty_subtitle),
                )
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filterItems, key = { it.key }) { filter ->
                    FilterChip(
                        selected = selectedFilterKey == filter.key,
                        onClick = { selectedFilterKey = filter.key },
                        label = { Text(text = stringResource(filter.labelRes)) },
                        leadingIcon = {
                            filter.icon?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }

        if (filteredMessages.isEmpty()) {
            item {
                EmptyMessagesCard()
            }
        } else {
            items(filteredMessages, key = { it.id }) { message ->
                MessageCard(
                    message = message,
                    onClick = { selectedMessageId = message.id }
                )
            }
        }
    }
}

@Composable
private fun MessagesOverviewRow(
    totalMessages: Int,
    unreadMessages: Int,
    actionMessages: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.MailOutline,
            value = totalMessages.toString(),
            label = stringResource(R.string.messages_stat_total)
        )
        OverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.NotificationsActive,
            value = unreadMessages.toString(),
            label = stringResource(R.string.messages_stat_unread)
        )
        OverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.TaskAlt,
            value = actionMessages.toString(),
            label = stringResource(R.string.messages_stat_action)
        )
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageCard(
    message: InboxMessageUi,
    onClick: () -> Unit
) {
    val isRead = message.isRead
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRead) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Icon(
                        imageVector = message.category.icon,
                        contentDescription = null,
                        tint = if (isRead) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = message.timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!isRead) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {}
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MessageBadge(
                    label = stringResource(message.category.labelRes),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (message.requiresAction) {
                    MessageBadge(
                        label = stringResource(R.string.messages_badge_action),
                        backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (!isRead) {
                    MessageBadge(
                        label = stringResource(R.string.messages_badge_unread),
                        backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Text(
                text = message.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.messages_action_open),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBadge(
    label: String,
    backgroundColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
private fun MessageDetailSheet(
    message: InboxMessageUi,
    onClose: () -> Unit,
    onToggleRead: () -> Unit,
    onOpenService: () -> Unit
) {
    val isRead = message.isRead
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.messages_detail_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (message.requiresAction) {
                MessageBadge(
                    label = stringResource(R.string.messages_badge_action),
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = message.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DetailMetaBlock(
            labelRes = R.string.messages_detail_from,
            value = message.sender
        )
        DetailMetaBlock(
            labelRes = R.string.messages_detail_time,
            value = message.timeLabel
        )
        DetailMetaBlock(
            labelRes = R.string.messages_detail_category,
            value = stringResource(message.category.labelRes)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.messages_action_close))
            }

            Button(
                onClick = onToggleRead,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        if (isRead) {
                            R.string.messages_action_mark_unread
                        } else {
                            R.string.messages_action_mark_read
                        }
                    )
                )
            }
        }

        if (message.relatedServiceId != null) {
            Button(
                onClick = onOpenService,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.messages_action_open_service))
            }
        }
    }
}

@Composable
private fun DetailMetaBlock(
    @StringRes labelRes: Int,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun EmptyMessagesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.messages_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.messages_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfflineNoticeCard(title: String, message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun messageFilterItems(): List<MessageFilterItem> {
    return buildList {
        add(
            MessageFilterItem(
                key = MessageFilterKey.ALL,
                labelRes = R.string.messages_filter_all
            )
        )
        add(
            MessageFilterItem(
                key = MessageFilterKey.UNREAD,
                labelRes = R.string.messages_filter_unread,
                icon = Icons.Filled.MailOutline
            )
        )

        MessageCategory.entries.forEach { category ->
            add(
                MessageFilterItem(
                    key = category.name,
                    labelRes = category.labelRes,
                    icon = category.icon
                )
            )
        }
    }
}

private object MessageFilterKey {
    const val ALL = "all"
    const val UNREAD = "unread"
}

private data class MessageFilterItem(
    val key: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector? = null
)
