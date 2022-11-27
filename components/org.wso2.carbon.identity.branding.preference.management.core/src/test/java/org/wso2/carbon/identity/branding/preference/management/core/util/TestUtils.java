/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.com).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.branding.preference.management.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;

/**
 * Test Utils for the branding preference manager tests.
 */
public class TestUtils {

    /**
     * Read preferences json in the samples.
     *
     * @param filename file name to be read.
     * @return Preferences object.
     * @throws IOException If error occurred while reading file.
     */
    public static Object getPreferenceFromFile(String filename) throws IOException {

        File sampleResourceFile = new File(getSamplesPath(filename));
        InputStream fileStream = FileUtils.openInputStream(sampleResourceFile);
        String preferencesJSON = convertInputStreamToString(fileStream);
        ObjectMapper mapper = new ObjectMapper();
        Object preference = mapper.readValue(preferencesJSON, Object.class);
        return preference;
    }

    /**
     * Get path for sample file.
     *
     * @param sampleName Sample file name.
     * @return Sample path.
     */
    private static String getSamplesPath(String sampleName) {

        if (StringUtils.isNotBlank(sampleName)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "samples",
                    sampleName).toString();
        }
        throw new IllegalArgumentException("Sample name cannot be empty.");
    }

    /**
     * This is used to convert input stream to a string.
     *
     * @param inputStream Event Publisher Configuration in as an input stream.
     * @throws IOException If error occurred while converting input stream to a string.
     */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }

}
