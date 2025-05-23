package org.wso2.carbon.identity.branding.preference.management.core.dao;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.database.utils.jdbc.NamedJdbcTemplate;
import org.wso2.carbon.identity.branding.preference.management.core.model.CustomLayoutContent;
import org.wso2.carbon.identity.core.util.JdbcUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AppCustomContentDAOImplTest {

    @Mock
    private NamedJdbcTemplate mockJdbcTemplate;

    private AppCustomContentDAOImpl appCustomContentDAO;

    private static final String TEST_APP_ID = "testAppId";
    private static final int TEST_TENANT_ID = 1;
    private static final String TEST_HTML_CONTENT = "<html></html>";
    private static final String TEST_CSS_CONTENT = "body { color: black; }";
    private static final String TEST_JS_CONTENT = "console.log('test');";

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        appCustomContentDAO = new AppCustomContentDAOImpl();
        mockStatic(JdbcUtils.class);
        when(JdbcUtils.getNewNamedJdbcTemplate()).thenReturn(mockJdbcTemplate);
    }

    @Test
    public void testAddAppCustomContent_success() throws Exception {
        CustomLayoutContent content = new CustomLayoutContent(TEST_HTML_CONTENT, TEST_CSS_CONTENT, TEST_JS_CONTENT);
        doNothing().when(mockJdbcTemplate).executeUpdate(any(), any());
        appCustomContentDAO.addAppCustomContent(content, TEST_APP_ID, TEST_TENANT_ID);
        verify(mockJdbcTemplate, times(3)).executeUpdate(any(), any());
    }
}
