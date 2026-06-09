package ru.truhot.rdang.сore.managers;

import org.bukkit.configuration.ConfigurationSection;
import ru.truhot.rdang.util.MessageUtil;
import ru.truhot.rdang.util.logger.Logger;

import java.util.ArrayList;
import java.util.List;

public class MessageManager {
    private List<String> openDungMessages;
    private String saveKeyMessage;
    private List<String> closedDungMessages;

    public void load(ConfigurationSection section) {
        openDungMessages = MessageUtil.colorize(section.getStringList("open_dung"));
        if (openDungMessages == null || openDungMessages.isEmpty()) {
            Logger.warn("В секции messages нет open_dung или он пуст!");
            openDungMessages = new ArrayList<>();
        }

        saveKeyMessage = MessageUtil.colorize(section.getString("save_key"));
        if (saveKeyMessage == null || saveKeyMessage.isEmpty()) {
            Logger.warn("В секции messages нет save_key или он пуст!");
        }

        closedDungMessages = MessageUtil.colorize(section.getStringList("closed_dung"));
        if (closedDungMessages == null || closedDungMessages.isEmpty()) {
            Logger.warn("В секции messages нет closed_dung или он пуст!");
            closedDungMessages = new ArrayList<>();
        }
    }

    public List<String> getOpenDungMessages(String playerName) {
        if (openDungMessages == null) return new ArrayList<>();
        List<String> formatted = new ArrayList<>();
        for (String message : openDungMessages) {
            formatted.add(message.replace("{player}", playerName));
        }
        return formatted;
    }

    public String getSaveKeyMessage() {
        return saveKeyMessage;
    }

    public List<String> getClosedDungMessages() {
        return closedDungMessages;
    }
}