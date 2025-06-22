# Discord Unknown User Notifications

## Overview

This feature automatically sends Discord channel notifications when an unknown user (not registered in the database) joins a voice channel. Instead of just logging the event, the system now proactively notifies administrators through a designated Discord channel.

## How It Works

1. **Event Detection**: When a Discord user joins or moves to a voice channel, the `VoiceChannelListener` captures the event
2. **User Lookup**: The system attempts to find the Discord user in the database using their Discord ID
3. **Unknown User Handling**: If no matching user is found, an `UnknownUserJoinEvent` is published
4. **Notification**: The `UnknownUserNotificationListener` handles the event and sends a formatted message to the configured notification channel

## Configuration

Add the following to your `application.yml`:

```yaml
discord:
  bot:
    enabled: true
    token: ${DISCORD_BOT_TOKEN}
    guild-id: ${DISCORD_GUILD_ID}
    notification-channel-id: ${DISCORD_NOTIFICATION_CHANNEL_ID}
```

### Getting the Channel ID

1. Enable Developer Mode in Discord (User Settings → Advanced → Developer Mode)
2. Right-click on your notification channel (e.g., `#admin-alerts`)
3. Select "Copy Channel ID"
4. Use this ID in your configuration

## Notification Message Format

The notification includes:
- User's Discord display name and username
- Discord user ID
- Voice channel name and server name
- Join timestamp
- Current channel member count
- Admin guidance message

Example message:
```
⚠️ **미등록 사용자 음성채널 입장 알림**

• **사용자**: John Doe (johndoe)
• **디스코드 ID**: 123456789012345678
• **입장 채널**: General Voice
• **서버**: Study Server
• **입장 시간**: 2024-01-15 14:30:25
• **현재 채널 인원**: 3명

📋 이 사용자는 시스템에 등록되어 있지 않습니다. 필요 시 사용자 등록을 진행해주세요.
```

## Components

### New Classes
- `UnknownUserJoinEvent`: Event object containing unknown user information
- `UnknownUserNotificationListener`: Event handler that sends Discord notifications

### Modified Classes
- `DiscordProperties`: Added `notificationChannelId` property
- `DiscordBotService`: Added channel messaging capabilities
- `VoiceChannelEventService`: Added unknown user event publishing

## Bot Permissions

Ensure your Discord bot has the following permissions in the notification channel:
- View Channel
- Send Messages
- Read Message History

## Testing

To test the feature:
1. Configure a test notification channel
2. Have an unregistered Discord user join a voice channel
3. Check that the notification appears in your configured channel

## Troubleshooting

- **No notifications received**: Check bot permissions and channel ID configuration
- **Bot can't find channel**: Verify the channel ID is correct and the bot is in the same server
- **Messages not formatted**: Ensure the bot has embed permissions if using rich formatting

## Future Enhancements

Potential improvements:
- Configurable message templates
- Multiple notification channels for different events
- Integration with user registration workflows
- Rate limiting for frequent unknown user events 