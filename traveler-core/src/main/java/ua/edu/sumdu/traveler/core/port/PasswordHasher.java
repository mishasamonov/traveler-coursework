package ua.edu.sumdu.traveler.core.port;

/** One-way password hashing; the web module implements it with BCrypt from Spring Security. */
public interface PasswordHasher {

  String hash(String rawPassword);
}
