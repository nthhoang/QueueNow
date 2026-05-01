package com.example.queuenow.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.queuenow.data.model.QueueTicket
import com.example.queuenow.data.model.TicketStatus
import com.example.queuenow.ui.theme.*
import com.example.queuenow.utils.toFormattedDate
import com.example.queuenow.utils.toStatusLabel

@Composable
fun QueueTicketCard(
    ticket: QueueTicket,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (ticket.status) {
        TicketStatus.CALLED.name          -> StatusCalled
        TicketStatus.COMPLETED.name       -> StatusCompleted
        TicketStatus.CANCELED.name        -> StatusCanceled
        TicketStatus.SKIPPED.name         -> StatusSkipped
        TicketStatus.PENDING_PAYMENT.name -> StatusCalled.copy(alpha = 0.7f)
        else                              -> StatusWaiting
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Số thứ tự badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = statusColor.copy(alpha = 0.12f),
                modifier = Modifier.size(62.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(ticket.ticketNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold, color = statusColor)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Tên địa điểm
                if (ticket.placeName.isNotEmpty()) {
                    Text(ticket.placeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = OnBackground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                // Tên phòng chờ
                if (ticket.roomName.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MeetingRoom, null,
                            tint = Primary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(ticket.roomName,
                            style = MaterialTheme.typography.bodySmall, color = Primary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null,
                        tint = TextSecondary, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(ticket.issueTime.toFormattedDate("HH:mm · dd/MM/yyyy"),
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                StatusChip(ticket.status)
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Filled.ChevronRight, null,
                    tint = Divider, modifier = Modifier.size(20.dp))
            }
        }
    }
}