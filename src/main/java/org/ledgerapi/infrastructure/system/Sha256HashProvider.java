package org.ledgerapi.infrastructure.system;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.ledgerapi.provider.HashProvider;

public final class Sha256HashProvider implements HashProvider {
  @Override
  public String sha256Hex(byte[] input) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required by every Java platform", e);
    }
  }
}
