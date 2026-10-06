package com.paymentprocessing.util;

import java.util.UUID;

public record CurrentUser(UUID id, String email) {
}
