package com.hansung.tracktory.domain.user.service;

import java.util.Locale;

public final class UserEmailNormalizer {

    private UserEmailNormalizer() {
    }

    public static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
