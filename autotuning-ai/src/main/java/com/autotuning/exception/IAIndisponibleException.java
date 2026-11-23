package com.autotuning.exception;

/** Levée quand l'API IA est injoignable, trop lente, non configurée ou renvoie une réponse inexploitable. */
public class IAIndisponibleException extends RuntimeException {
    public IAIndisponibleException(String message) { super(message); }
    public IAIndisponibleException(String message, Throwable cause) { super(message, cause); }
}
