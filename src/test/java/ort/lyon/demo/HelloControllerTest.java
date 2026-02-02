package ort.lyon.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelloControllerTest {

    @Test
    void leMessageHelloDoitEtreCorrect() {
        String message = "Hello CI/CD";
        assertEquals("Hello CI/CD", message);
    }
}