package io.github.cspigot.utils.netty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Creaxx
 * Created At: 5/8/2023
 */

@Getter
@RequiredArgsConstructor
public enum ByteResponseType {
    SUCCESS("Success"),
    TOO_MANY_IPS("Too many IPs"),
    BALLS("cock"),
    TOO_MANY_REQUESTS("Too many Requests");

    private final String displayName;
}
