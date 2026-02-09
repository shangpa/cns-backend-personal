package com.example.springjwt.fridge;

import com.example.springjwt.User.UserEntity;
import com.example.springjwt.notification.FCMService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FridgeExpiryScheduler {

    private final FridgeService fridgeService;
    private final FCMService fcmService;

    // 매일 오전 9시에 실행
    @Scheduled(cron = "0 0 9 * * *")
    public void notifyExpiringItems() {
        List<Fridge> items = fridgeService.getExpiringFridgeItems(3);

        for (Fridge fridge : items) {
            UserEntity user = fridge.getUser();
            String content = "냉장고에 있는 '" + fridge.getIngredientName() + "'의 유통기한이 3일 이내입니다.";
            fcmService.sendNotificationToUser(user, "냉장고 알림", content, "FRIDGE");
        }
    }
}