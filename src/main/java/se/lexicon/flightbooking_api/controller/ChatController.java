package se.lexicon.flightbooking_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.lexicon.flightbooking_api.dto.ChatRequestDTO;
import se.lexicon.flightbooking_api.dto.ChatResponseDTO;
import se.lexicon.flightbooking_api.service.FlightAssistantService;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Flight Assistant Chat", description = "AI assistant for flight booking operations")
public class ChatController {

    private final FlightAssistantService assistantService;

    @Operation(summary = "Chat with AI assistant", description = "Send a message to the AI assistant for flight booking help")
    @PostMapping
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : "default";
        String response = assistantService.processMessage(request.message(), sessionId);
        return ResponseEntity.ok(new ChatResponseDTO(response, sessionId));
    }
}