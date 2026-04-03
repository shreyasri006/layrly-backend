package com.layrly.ai;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.layrly.dao.WardrobeItemDAO;
import com.layrly.domain.WardrobeAnalyzedItem;
import com.layrly.domain.WardrobeItem;
import com.layrly.lambda.WardrobeLambdaHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.Map;

import static com.layrly.ai.Prompts.RECOMMENDATION_PROMPT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImageAnalyzerTest {
    private ObjectMapper mapper = new ObjectMapper();

    // @Test
    void generateRecommendation() throws Exception {
        String wardrobeItems = """
                [
                  {
                    "apparel_id": 3,
                    "description": "Classic white Oxford button-down shirt with a spread collar.",
                    "items": [{ "type": "shirt", "color": "white", "color_family": "neutral", "pattern": "solid", "material": "cotton", "style": "formal", "fit": "slim", "layer": "base", "formality_level": 4, "season": ["spring","summer","fall","winter"], "temperature_range_f": [60,85], "occasion": ["work","business casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 4,
                    "description": "Light blue chambray shirt with a relaxed, lived-in look.",
                    "items": [{ "type": "shirt", "color": "light blue", "color_family": "blue", "pattern": "solid", "material": "chambray", "style": "casual", "fit": "relaxed", "layer": "base", "formality_level": 2, "season": ["spring","summer"], "temperature_range_f": [65,90], "occasion": ["casual","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 5,
                    "description": "Burgundy and navy plaid flannel shirt.",
                    "items": [{ "type": "shirt", "color": "burgundy/navy", "color_family": "red", "pattern": "plaid", "material": "flannel", "style": "casual", "fit": "regular", "layer": "mid", "formality_level": 2, "season": ["fall","winter"], "temperature_range_f": [40,65], "occasion": ["casual","outdoor"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 6,
                    "description": "Beige linen short-sleeve button-up shirt.",
                    "items": [{ "type": "shirt", "color": "beige", "color_family": "neutral", "pattern": "solid", "material": "linen", "style": "casual", "fit": "relaxed", "layer": "base", "formality_level": 2, "season": ["spring","summer"], "temperature_range_f": [72,100], "occasion": ["casual","beach","vacation"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 7,
                    "description": "Navy and white Breton striped T-shirt.",
                    "items": [{ "type": "t-shirt", "color": "navy/white", "color_family": "blue", "pattern": "stripes", "material": "cotton", "style": "casual", "fit": "regular", "layer": "base", "formality_level": 1, "season": ["spring","summer"], "temperature_range_f": [65,90], "occasion": ["casual","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 8,
                    "description": "Classic tan shearling-collar trucker jacket in washed denim.",
                    "items": [{ "type": "jacket", "color": "tan/denim", "color_family": "neutral", "pattern": "solid", "material": "denim", "style": "casual", "fit": "regular", "layer": "outer", "formality_level": 2, "season": ["fall","spring"], "temperature_range_f": [45,65], "occasion": ["casual","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 9,
                    "description": "Olive green wax cotton field jacket with patch pockets.",
                    "items": [{ "type": "jacket", "color": "olive", "color_family": "green", "pattern": "solid", "material": "wax cotton", "style": "casual", "fit": "regular", "layer": "outer", "formality_level": 2, "season": ["fall","spring"], "temperature_range_f": [45,65], "occasion": ["casual","outdoor"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 10,
                    "description": "Black leather moto jacket with zip detailing.",
                    "items": [{ "type": "jacket", "color": "black", "color_family": "black", "pattern": "solid", "material": "leather", "style": "casual", "fit": "slim", "layer": "outer", "formality_level": 3, "season": ["fall","spring"], "temperature_range_f": [48,68], "occasion": ["casual","night out"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 11,
                    "description": "Red and black buffalo plaid shirt jacket (shacket).",
                    "items": [{ "type": "shacket", "color": "red/black", "color_family": "red", "pattern": "plaid", "material": "flannel", "style": "casual", "fit": "relaxed", "layer": "outer", "formality_level": 2, "season": ["fall","spring"], "temperature_range_f": [45,65], "occasion": ["casual","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 12,
                    "description": "Navy single-breasted wool blazer with gold buttons.",
                    "items": [{ "type": "blazer", "color": "navy", "color_family": "blue", "pattern": "solid", "material": "wool", "style": "smart casual", "fit": "slim", "layer": "outer", "formality_level": 4, "season": ["fall","winter","spring"], "temperature_range_f": [50,75], "occasion": ["work","business casual","formal"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 13,
                    "description": "Charcoal grey herringbone tweed blazer.",
                    "items": [{ "type": "blazer", "color": "charcoal", "color_family": "grey", "pattern": "herringbone", "material": "tweed", "style": "smart casual", "fit": "regular", "layer": "outer", "formality_level": 4, "season": ["fall","winter"], "temperature_range_f": [40,65], "occasion": ["work","business casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 14,
                    "description": "Tan linen unstructured blazer for warm weather.",
                    "items": [{ "type": "blazer", "color": "tan", "color_family": "neutral", "pattern": "solid", "material": "linen", "style": "smart casual", "fit": "relaxed", "layer": "outer", "formality_level": 3, "season": ["spring","summer"], "temperature_range_f": [68,90], "occasion": ["business casual","vacation","casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 15,
                    "description": "Classic grey pullover hoodie in heavyweight fleece.",
                    "items": [{ "type": "hoodie", "color": "heather grey", "color_family": "grey", "pattern": "solid", "material": "cotton fleece", "style": "casual", "fit": "regular", "layer": "mid", "formality_level": 1, "season": ["fall","winter","spring"], "temperature_range_f": [45,65], "occasion": ["casual","loungewear"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 16,
                    "description": "Forest green full-zip hoodie with kangaroo pocket.",
                    "items": [{ "type": "hoodie", "color": "forest green", "color_family": "green", "pattern": "solid", "material": "cotton blend", "style": "casual", "fit": "regular", "layer": "mid", "formality_level": 1, "season": ["fall","winter","spring"], "temperature_range_f": [45,68], "occasion": ["casual","outdoor","sport"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 17,
                    "description": "Oversized black cropped hoodie with raw-edge hem.",
                    "items": [{ "type": "hoodie", "color": "black", "color_family": "black", "pattern": "solid", "material": "fleece", "style": "streetwear", "fit": "oversized", "layer": "mid", "formality_level": 1, "season": ["fall","winter"], "temperature_range_f": [45,65], "occasion": ["casual","streetwear"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 18,
                    "description": "Dark indigo straight-leg denim jeans.",
                    "items": [{ "type": "jeans", "color": "dark indigo", "color_family": "blue", "pattern": "solid", "material": "denim", "style": "casual", "fit": "straight", "layer": "base", "formality_level": 2, "season": ["spring","summer","fall","winter"], "temperature_range_f": [40,80], "occasion": ["casual","business casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 19,
                    "description": "Slim-fit navy chinos in a stretch cotton blend.",
                    "items": [{ "type": "chinos", "color": "navy", "color_family": "blue", "pattern": "solid", "material": "cotton blend", "style": "smart casual", "fit": "slim", "layer": "base", "formality_level": 3, "season": ["spring","summer","fall"], "temperature_range_f": [55,85], "occasion": ["work","business casual","casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 20,
                    "description": "Charcoal grey slim-fit dress trousers.",
                    "items": [{ "type": "trousers", "color": "charcoal", "color_family": "grey", "pattern": "solid", "material": "wool blend", "style": "formal", "fit": "slim", "layer": "base", "formality_level": 5, "season": ["fall","winter","spring"], "temperature_range_f": [50,75], "occasion": ["work","formal","interview"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 21,
                    "description": "Khaki relaxed-fit cargo pants with side utility pockets.",
                    "items": [{ "type": "cargo pants", "color": "khaki", "color_family": "neutral", "pattern": "solid", "material": "cotton ripstop", "style": "casual", "fit": "relaxed", "layer": "base", "formality_level": 1, "season": ["spring","summer","fall"], "temperature_range_f": [55,85], "occasion": ["casual","outdoor","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 22,
                    "description": "Olive green cargo shorts with multiple utility pockets.",
                    "items": [{ "type": "shorts", "color": "olive", "color_family": "green", "pattern": "solid", "material": "cotton", "style": "casual", "fit": "regular", "layer": "base", "formality_level": 1, "season": ["spring","summer"], "temperature_range_f": [70,100], "occasion": ["casual","outdoor","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 23,
                    "description": "Navy swim trunks with a quick-dry tropical print.",
                    "items": [{ "type": "swim shorts", "color": "navy/multicolor", "color_family": "blue", "pattern": "tropical", "material": "polyester", "style": "casual", "fit": "regular", "layer": "base", "formality_level": 1, "season": ["summer"], "temperature_range_f": [80,100], "occasion": ["beach","vacation","pool"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 24,
                    "description": "Heather grey athletic shorts with a drawstring waist.",
                    "items": [{ "type": "athletic shorts", "color": "heather grey", "color_family": "grey", "pattern": "solid", "material": "polyester", "style": "sport", "fit": "regular", "layer": "base", "formality_level": 1, "season": ["spring","summer"], "temperature_range_f": [65,100], "occasion": ["sport","gym","casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 25,
                    "description": "White low-top leather sneakers with minimal branding.",
                    "items": [{ "type": "sneakers", "color": "white", "color_family": "neutral", "pattern": "solid", "material": "leather", "style": "casual", "fit": "regular", "layer": "base", "formality_level": 2, "season": ["spring","summer","fall"], "temperature_range_f": [50,95], "occasion": ["casual","business casual","weekend"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 26,
                    "description": "Tan suede Chelsea boots with an elastic side panel.",
                    "items": [{ "type": "chelsea boots", "color": "tan", "color_family": "neutral", "pattern": "solid", "material": "suede", "style": "smart casual", "fit": "regular", "layer": "base", "formality_level": 3, "season": ["fall","winter","spring"], "temperature_range_f": [35,68], "occasion": ["work","casual","business casual"], "confidence": "high" }]
                  },
                  {
                    "apparel_id": 27,
                    "description": "Dark brown leather Oxford dress shoes with broguing.",
                    "items": [{ "type": "oxford shoes", "color": "dark brown", "color_family": "neutral", "pattern": "brogue", "material": "leather", "style": "formal", "fit": "regular", "layer": "base", "formality_level": 5, "season": ["fall","winter","spring"], "temperature_range_f": [40,75], "occasion": ["formal","work","interview"], "confidence": "high" }]
                  }
                ]
                    """;

        String weather = """
                    { "temperature": 66.0, "feelsLike": 66.0, "condition": "Overcast", "icon": "https://cdn.weatherapi.com/weather/64x64/night/122.png", "windMph": 5.1, "location": "San Gabriel, California", "description": "Overcast with light breeze" }
                    """;

        String prompt = RECOMMENDATION_PROMPT.replace("{{WARDROBE_JSON}}", wardrobeItems)
                .replace("{{WEATHER_JSON}}", weather);
        String result = new ImageAnalyzer().generateRecommendation(prompt);
        System.out.println(result);
    }
}
