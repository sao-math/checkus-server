package saomath.checkusserver.notification.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import saomath.checkusserver.discord.service.DiscordBotService;
import saomath.checkusserver.notification.dto.NotificationMessage;
import saomath.checkusserver.notification.formatter.MessageFormatter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordNotificationChannelTest {

    @Mock
    private DiscordBotService discordBotService;

    @Mock
    private MessageFormatter messageFormatter;

    private DiscordNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new DiscordNotificationChannel(discordBotService, messageFormatter);
    }

    @Test
    void testGetChannelType() {
        assertThat(channel.getChannelType()).isEqualTo("DISCORD");
    }

    @Test
    void testIsEnabled_WhenDiscordBotServiceExists() {
        assertThat(channel.isEnabled()).isTrue();
    }

    @Test
    void testIsEnabled_WhenDiscordBotServiceIsNull() {
        channel = new DiscordNotificationChannel(null, messageFormatter);
        assertThat(channel.isEnabled()).isFalse();
    }

    @Test
    void testValidateRecipient_ValidId() {
        assertThat(channel.validateRecipient("123456789")).isTrue();
    }

    @Test
    void testValidateRecipient_InvalidId() {
        assertThat(channel.validateRecipient("invalid")).isFalse();
        assertThat(channel.validateRecipient(null)).isFalse();
        assertThat(channel.validateRecipient("")).isFalse();
        assertThat(channel.validateRecipient("   ")).isFalse();
    }

    @Test
    void testSupportsRichFormatting() {
        when(messageFormatter.supportsMarkdown()).thenReturn(true);
        assertThat(channel.supportsRichFormatting()).isTrue();
    }

    @Test
    void testSupportsPriority() {
        assertThat(channel.supportsPriority()).isFalse();
    }

    @Test
    void testSendMessage_Success() {
        // Given
        NotificationMessage message = createTestNotification("123456789");
        String formattedMessage = "Formatted test message";
        
        when(messageFormatter.format(message)).thenReturn(formattedMessage);
        when(messageFormatter.getMaxMessageLength()).thenReturn(2000);
        when(discordBotService.sendDirectMessage("123456789", formattedMessage))
                .thenReturn(CompletableFuture.completedFuture(true));

        // When
        CompletableFuture<Boolean> result = channel.sendMessage(message);

        // Then
        assertThat(result.join()).isTrue();
        verify(messageFormatter).format(message);
        verify(discordBotService).sendDirectMessage("123456789", formattedMessage);
    }

    @Test
    void testSendMessage_InvalidRecipient() {
        // Given
        NotificationMessage message = createTestNotification("invalid");

        // When
        CompletableFuture<Boolean> result = channel.sendMessage(message);

        // Then
        assertThat(result.join()).isFalse();
        verifyNoInteractions(messageFormatter);
        verifyNoInteractions(discordBotService);
    }

    @Test
    void testSendMessage_ChannelDisabled() {
        // Given
        channel = new DiscordNotificationChannel(null, messageFormatter);
        NotificationMessage message = createTestNotification("123456789");

        // When
        CompletableFuture<Boolean> result = channel.sendMessage(message);

        // Then
        assertThat(result.join()).isFalse();
        verifyNoInteractions(messageFormatter);
        verifyNoInteractions(discordBotService);
    }

    @Test
    void testSendMessage_MessageTooLong() {
        // Given
        NotificationMessage message = createTestNotification("123456789");
        String longMessage = "A".repeat(2500); // 최대 길이 초과
        String suffix = "\n\n... (메시지가 잘렸습니다)";
        int targetLength = 2000 - suffix.length();
        String truncatedMessage = "A".repeat(targetLength) + suffix;
        
        when(messageFormatter.format(message)).thenReturn(longMessage);
        when(messageFormatter.getMaxMessageLength()).thenReturn(2000);
        when(discordBotService.sendDirectMessage(eq("123456789"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));

        // When
        CompletableFuture<Boolean> result = channel.sendMessage(message);

        // Then
        assertThat(result.join()).isTrue();
        verify(discordBotService).sendDirectMessage(eq("123456789"), eq(truncatedMessage));
    }

    @Test
    void testSendMessage_ExceptionHandling() {
        // Given
        NotificationMessage message = createTestNotification("123456789");
        
        when(messageFormatter.format(message)).thenThrow(new RuntimeException("Format error"));

        // When
        CompletableFuture<Boolean> result = channel.sendMessage(message);

        // Then
        assertThat(result.join()).isFalse();
    }

    @Test
    void testSendBatchMessages_Success() {
        // Given
        List<NotificationMessage> messages = List.of(
                createTestNotification("123456789"),
                createTestNotification("987654321")
        );
        
        when(messageFormatter.format(any())).thenReturn("Test message");
        when(messageFormatter.getMaxMessageLength()).thenReturn(2000);
        when(discordBotService.sendDirectMessage(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(true));

        // When
        CompletableFuture<Integer> result = channel.sendBatchMessages(messages);

        // Then
        assertThat(result.join()).isEqualTo(2);
        verify(discordBotService, times(2)).sendDirectMessage(anyString(), anyString());
    }

    @Test
    void testSendBatchMessages_ChannelDisabled() {
        // Given
        channel = new DiscordNotificationChannel(null, messageFormatter);
        List<NotificationMessage> messages = List.of(createTestNotification("123456789"));

        // When
        CompletableFuture<Integer> result = channel.sendBatchMessages(messages);

        // Then
        assertThat(result.join()).isEqualTo(0);
        verifyNoInteractions(discordBotService);
    }

    private NotificationMessage createTestNotification(String recipientId) {
        return NotificationMessage.builder()
                .type(NotificationMessage.NotificationType.STUDY_START)
                .title("Test Title")
                .message("Test Message")
                .data(Map.of("subject", "테스트", "endTime", LocalDateTime.now()))
                .timestamp(LocalDateTime.now())
                .priority(2)
                .recipientId(recipientId)
                .recipientName("테스트 사용자")
                .build();
    }
} 