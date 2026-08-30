package com.falconenergy.service;

import jakarta.servlet.http.HttpServletRequest;

public interface PawaPayCallbackVerifier {
    void verify(HttpServletRequest request, byte[] body);
}
