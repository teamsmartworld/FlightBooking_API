package se.lexicon.flightbooking_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Service;
import se.lexicon.flightbooking_api.dto.AvailableFlightDTO;
import se.lexicon.flightbooking_api.dto.BookFlightRequestDTO;
import se.lexicon.flightbooking_api.dto.FlightBookingDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class FlightAssistantService {

    private final OpenAiChatModel chatModel;
    private final FlightBookingService flightBookingService;
    private final Map<String, List<Message>> chatHistory = new ConcurrentHashMap<>();
    private final int MAX_HISTORY = 10;

    private final String SYSTEM_MESSAGE = """
        You are a helpful flight booking assistant. You can help users with:
        1. Finding available flights
        2. Booking flights (requires passenger name and email)
        3. Checking existing bookings by email
        4. Cancelling bookings (requires flight ID and email)
        
        Always be polite and guide users step by step. If information is missing, ask for it.
        Use the available tools to perform actions when needed.
        """;

    public String processMessage(String message, String sessionId) {
        List<Message> history = chatHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        if (history.isEmpty()) {
            history.add(new SystemMessage(SYSTEM_MESSAGE));
        }
        
        history.add(new UserMessage(message));
        
        ChatClient chatClient = ChatClient.builder(chatModel)
            .defaultFunctions("searchFlights", "bookFlight", "getBookings", "cancelBooking")
            .build();
        
        String response = chatClient.prompt()
            .messages(history)
            .functions("searchFlights", "bookFlight", "getBookings", "cancelBooking")
            .call()
            .content();
        
        history.add(new UserMessage("Assistant: " + response));
        
        // Keep history limited
        if (history.size() > MAX_HISTORY) {
            history.subList(1, history.size() - MAX_HISTORY + 1).clear();
        }
        
        return response;
    }

    @Function("Search for available flights")
    public List<AvailableFlightDTO> searchFlights() {
        return flightBookingService.findAvailableFlights();
    }

    @Function("Book a flight with passenger details")
    public FlightBookingDTO bookFlight(Long flightId, String passengerName, String passengerEmail) {
        return flightBookingService.bookFlight(flightId, new BookFlightRequestDTO(passengerName, passengerEmail));
    }

    @Function("Get all bookings for an email")
    public List<FlightBookingDTO> getBookings(String email) {
        return flightBookingService.findBookingsByEmail(email);
    }

    @Function("Cancel a flight booking")
    public String cancelBooking(Long flightId, String email) {
        flightBookingService.cancelFlight(flightId, email);
        return "Flight booking cancelled successfully";
    }
}