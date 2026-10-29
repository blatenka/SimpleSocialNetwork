package com.aloute;

import com.aloute.repository.mongo.NotificationRepository;
import com.aloute.service.UserService;
import com.aloute.service.impl.NotificationServiceImpl;
import com.aloute.util.SecurityUtils;
import com.aloute.websocket.RealtimeEventPublisher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void notificationPagesAreRequestedNewestFirstWithStableIdTieBreak() {
        NotificationRepository repository = mock(NotificationRepository.class);
        UserService userService = mock(UserService.class);
        NotificationServiceImpl service = new NotificationServiceImpl(
                repository, mock(MongoTemplate.class), userService, mock(SecurityUtils.class),
                mock(RealtimeEventPublisher.class));
        when(repository.findByReceiverIdOrderByCreatedAtDescIdDesc(eq(4L), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(userService.getUserSummaries(List.of())).thenReturn(List.of());

        service.getNotifications(4L, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByReceiverIdOrderByCreatedAtDescIdDesc(eq(4L), pageable.capture());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.DESC, pageable.getValue().getSort().getOrderFor("id").getDirection());
    }
}
