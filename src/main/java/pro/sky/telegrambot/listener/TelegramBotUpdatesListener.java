package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.sun.istack.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import pro.sky.telegrambot.component.SendHelper;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import static java.time.LocalDateTime.parse;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
      private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final static String WELCOME_TEXT =
            "Привет! Это планинг-бот:)! Для планирования задачи отправьте её в формате: 01.01.2022 20:00 Сделать домашнюю работу";
    private static final Pattern PATTERN = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

//    @Autowired

    private final TelegramBot telegramBot;
    private final NotificationTaskService notificationTaskService;
    private  final SendHelper sendHelper;

    public TelegramBotUpdatesListener(TelegramBot telegramBot,
                                      NotificationTaskService notificationTaskService,
                                      SendHelper sendHelper) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
        this.sendHelper = sendHelper;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                logger.info("Processing update: {}", update);
                // Process your updates here
                    String text = update.message().text();
                    Long chatId = update.message().chat().id();
//      Приветственное сообщение при /start
                    if ("/start".equals(text)) {
                        sendHelper.sendMessage (chatId, WELCOME_TEXT);
                    } else {
                        Matcher matcher = PATTERN.matcher(text);
                        LocalDateTime localDateTime;
//       Отправление сообщения в формате "01.01.2022 20:00 Сделать домашнюю работу"
                        if (matcher.find() &&  (localDateTime = parse(matcher.group(1))) !=null) {
                            String message = matcher.group(3);
                            notificationTaskService.create(chatId, message, localDateTime);
                            sendHelper.sendMessage(chatId, "Задача запланирована");
                        } else {
                            sendHelper.sendMessage(chatId, "Некорректный формат сообщения!");
                        }
                    }

            });
        } catch (Exception e){
            logger.error(e.getMessage(),e);
            e.printStackTrace();
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
    @Nullable
    private LocalDateTime parse(String localDateTime){
        try {
            return LocalDateTime.parse(localDateTime, FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
