package com.org.cmbms.history.service;

import com.org.cmbms.history.model.RequestHistory;
import com.org.cmbms.history.repository.RequestHistoryRepository;
import com.org.cmbms.user.model.User;
import com.org.cmbms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestHistoryService {
    private final RequestHistoryRepository requestHistoryRepository;
    private final UserRepository userRepository;

    public void recordHistory(Long requestId, String requestType, String action, String status, String actorName, Long actorId, String note) {
        RequestHistory history = new RequestHistory();
        history.setRequestId(requestId);
        history.setRequestType(requestType.toUpperCase());
        history.setAction(action);
        history.setStatus(status);
        
        String resolvedName = actorName;
        if ((resolvedName == null || resolvedName.isBlank()) && actorId != null && actorId > 0) {
            resolvedName = userRepository.findById(actorId)
                    .map(User::getName)
                    .orElse("Unknown User");
        }
        
        history.setActorName(resolvedName != null ? resolvedName : "System");
        history.setActorId(actorId);
        history.setNote(note);
        history.setCreatedAt(LocalDateTime.now());
        requestHistoryRepository.save(history);
    }

    public List<RequestHistory> getHistory(Long requestId, String requestType) {
        return requestHistoryRepository.findByRequestIdAndRequestTypeOrderByCreatedAtDesc(requestId, requestType.toUpperCase());
    }
}
