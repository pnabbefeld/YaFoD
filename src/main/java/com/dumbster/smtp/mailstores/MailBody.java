/*
 * Copyright 2020 Peter Nabbefeld.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp.mailstores;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;

/**
 *
 * @author Peter Nabbefeld
 */
class MailBody {

    private static class ContentType {

        private static final String MULTIPART_TYPENAME = "multipart/";

        private final String contentTypeHeader;
        private String multipartSubtype;
        private String boundary;

        public ContentType(String contentTypeHeader) {
            this.contentTypeHeader = contentTypeHeader;
            if (contentTypeHeader.startsWith("multipart/")) {
                int p = contentTypeHeader.indexOf(';');
                multipartSubtype = contentTypeHeader.substring(MULTIPART_TYPENAME.length(), p);
                String test = contentTypeHeader.substring(p + 1).trim();
                p = test.indexOf('"');
                if (!"boundary=".equals(test.substring(0, p))) {
                    throw new IllegalArgumentException("Parameter 'boundary' expected, bbut found " + test.substring(0, p - 1));
                }
                p++;
                int q = test.indexOf('"', p);
                boundary = test.substring(p, q);
            }
        }

        public boolean isMultipartType() {
            return multipartSubtype != null;
        }

        public String getMultipartSubype() {
            return multipartSubtype;
        }

        public String getBoundary() {
            return boundary;
        }
    }

    private static class BodyPart {

        private final Map<String, String> headers;
        private final List<String> names = new ArrayList<>();
        private final String text;

        private BodyPart(String text) throws MessagingException {
            int p = text.indexOf("\n\n");
            this.headers = getHeaderFields(text.substring(0, p));
            String cte = headers.get("Content-Transfer-Encoding");
            if (cte != null && "quoted-printable".equals(cte)) {
                this.text = convertFromQuotedPrintable(text.substring(p + 2));
                headers.remove("Content-Transfer-Encoding");
            } else {
                this.text = text.substring(p + 2);
            }
        }

        private Map<String, String> getHeaderFields(String text) {
            Map<String, String> map = new TreeMap<>();
            StringTokenizer tk = new StringTokenizer(text, "\n");
            String hdr;
            int p;
            while (tk.hasMoreTokens()) {
                hdr = tk.nextToken();
                p = hdr.indexOf(':');
                map.put(hdr.substring(0, p), hdr.substring(p + 1).trim());
                names.add(hdr.substring(0, p));
            }
            return map;
        }

        private String convertFromQuotedPrintable(String body) throws MessagingException {
            InputStream raw = new ByteArrayInputStream(body.getBytes(StandardCharsets.ISO_8859_1));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream is = MimeUtility.decode(raw, "quoted-printable");
            int x;
            try {
                while ((x = is.read()) != -1) {
                    baos.write(x);
                }
            } catch (IOException ex) {
                throw new MessagingException("Could not convert message body", ex);
            }
            return baos.toString();
        }

        private String asJoinedString() {
            StringBuilder sb = new StringBuilder();
            for (String name : names) {
                sb.append(name).append(": ").append(headers.get(name)).append('\n');
            }
            sb.append('\n').append(text);
            return sb.toString();
        }
    }

    private final ContentType contentType;
    private final List<BodyPart> bodyParts;

    MailBody(String body, String[] contentType) throws MessagingException {
        this.contentType = new ContentType(contentType[0]);
        this.bodyParts = getBodyParts(body, this.contentType);
    }

    CharSequence asJoinedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(contentType.getBoundary());
        for (BodyPart bodyPart : bodyParts) {
            sb.append('\n').append(bodyPart.asJoinedString()).append('\n').append(contentType.getBoundary());
        }
        sb.append("--\n");
        return sb.toString();
    }

    private List<BodyPart> getBodyParts(String body, ContentType contentType) throws MessagingException {
        List<BodyPart> parts = new ArrayList<>();
        if (contentType.isMultipartType()) {
            String lastBoundary = contentType.getBoundary() + "--\n";
            if (!body.startsWith("--" + contentType.getBoundary()) || !body.endsWith(lastBoundary)) {
                throw new MessagingException("Multipart body without boundaries");
            }
            int x = contentType.getBoundary().length();
            String test = body.substring(x + 3, body.length() - x);
            String mtch = "\n" + contentType.getBoundary();
            int p = 0;
            int q = test.indexOf(mtch, p);
            for (;;) {
                if (q < 0) {
                    parts.add(new BodyPart(test.substring(p)));
                    break;
                } else {
                    parts.add(new BodyPart(test.substring(p, q + 2)));
                    p = q + x + 4;
                }
            }
        } else {
            parts.add(new BodyPart(body));
        }
        return parts;
    }
}
