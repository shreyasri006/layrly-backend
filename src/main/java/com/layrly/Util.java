package com.layrly;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Util {
    public static final ObjectMapper mapper = new ObjectMapper();
    public static final String S3_BUCKET_NAME = System.getProperty("S3_BUCKET_NAME", "layrly");
    public static final String CLOUDFRONT_DOMAIN = System.getProperty("CLOUDFRONT_DOMAIN", "https://d2jmh09mtfpmv0.cloudfront.net");

}
