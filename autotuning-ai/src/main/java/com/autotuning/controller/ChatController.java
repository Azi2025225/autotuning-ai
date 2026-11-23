package com.autotuning.controller;

import com.autotuning.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class ChatController {
    public record ChatRequest(String message, List<ChatService.Tour> historique) { }

    private final ChatService chat;

    public ChatController(ChatService chat) { this.chat = chat; }

    @GetMapping("/chat")
    public String page(Model m) {
        m.addAttribute("suggestions", List.of(
                "Quels sont vos nouveaux produits ?",
                "Quel gain en Stage 1 sur une Golf 7 2.0 TDI 150 ch ?",
                "Où en est ma commande ?",
                "Quels kits carrosserie avez-vous et à quel prix ?",
                "Une reprogrammation annule-t-elle la garantie ?",
                "C'est quoi un Stage 1, 2 et 3 ?"));
        return "chat/chat";
    }

    @PostMapping(value = "/chat/message", consumes = "application/json")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> message(Authentication auth, @RequestBody ChatRequest req) {
        try {
            ChatService.Reponse r = chat.repondre(auth.getName(), req.message(), req.historique());
            return ResponseEntity.ok(Map.<String, Object>of("reponse", r.texte(), "local", r.local()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.<String, Object>of("erreur", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(Map.<String, Object>of("erreur", e.getMessage()));
        }
    }
}
