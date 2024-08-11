/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.hdg.keklist.util;

import de.hdg.keklist.Keklist;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * Matches a request based on IP Address or subnet mask matching against the remote
 * address.
 * <p>
 * Both IPv6 and IPv4 addresses are supported, but a matcher which is configured with an
 * IPv4 address will never match a request which returns an IPv6 address, and vice-versa.
 *
 * @author Luke Taylor
 * @since 3.0.2
 */
public final class IpAddressMatcher {

    private static final Pattern IPV4 = Pattern.compile("\\d{0,3}.\\d{0,3}.\\d{0,3}.\\d{0,3}(/\\d{0,3})?");

    private final int nMaskBits;

    private final InetAddress requiredAddress;

    /**
     * Takes a specific IP address or a range specified using the IP/Netmask (e.g.
     * 192.168.1.0/24 or 202.24.0.0/14).
     * @param ipAddress the address or range of addresses from which the request must
     * come.
     */
    public IpAddressMatcher(String ipAddress) {
        assertNotHostName(ipAddress);
        if (ipAddress.indexOf('/') > 0) {
            String[] addressAndMask = ipAddress.split("/");
            ipAddress = addressAndMask[0];
            this.nMaskBits = Integer.parseInt(addressAndMask[1]);
        }
        else {
            this.nMaskBits = -1;
        }
        this.requiredAddress = parseAddress(ipAddress);
    }


    public boolean matches(String address) {
        assertNotHostName(address);
        InetAddress remoteAddress = parseAddress(address);
        if (!this.requiredAddress.getClass().equals(remoteAddress.getClass())) {
            return false;
        }
        if (this.nMaskBits < 0) {
            return remoteAddress.equals(this.requiredAddress);
        }
        byte[] remAddr = remoteAddress.getAddress();
        byte[] reqAddr = this.requiredAddress.getAddress();
        int nMaskFullBytes = this.nMaskBits / 8;
        byte finalByte = (byte) (0xFF00 >> (this.nMaskBits & 0x07));
        for (int i = 0; i < nMaskFullBytes; i++) {
            if (remAddr[i] != reqAddr[i]) {
                return false;
            }
        }
        if (finalByte != 0) {
            return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
        }
        return true;
    }

    private void assertNotHostName(String ipAddress) {
        boolean isIpv4 = IPV4.matcher(ipAddress).matches();
        if (isIpv4) {
            return;
        }
        String error = "ipAddress " + ipAddress + " doesn't look like an IP Address. Is it a host name?";
        if(ipAddress.charAt(0) == '[' || ipAddress.charAt(0) == ':'
                || (Character.digit(ipAddress.charAt(0), 16) != -1 && ipAddress.contains(":"))) {
            Keklist.getInstance().getSLF4JLogger().warn(error);
        }
    }

    private InetAddress parseAddress(String address) {
        try {
            return InetAddress.getByName(address);
        }
        catch (UnknownHostException ex) {
            throw new IllegalArgumentException("Failed to parse address '" + address + "'", ex);
        }
    }

}