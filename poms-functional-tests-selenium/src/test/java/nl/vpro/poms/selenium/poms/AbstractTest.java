package nl.vpro.poms.selenium.poms;

import io.github.bonigarcia.wdm.DriverManagerType;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.paulhammant.ngwebdriver.NgWebDriver;

import nl.vpro.api.client.utils.Config;
import nl.vpro.poms.selenium.pages.PomsLogin;
import nl.vpro.poms.selenium.pages.Search;
import nl.vpro.poms.selenium.util.WebDriverFactory.Browser;
import nl.vpro.rules.TestMDC;

/**
 *
 */
@RunWith(Parameterized.class)
public abstract class AbstractTest {
    static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);
    Logger log = LoggerFactory.getLogger(getClass());


    public static final Config CONFIG =
            new Config("npo-functional-tests.properties", "npo-browser-tests.properties");

    public static final String MID = "WO_VPRO_025057";

    @Rule
    public Timeout timeout = new Timeout(5, TimeUnit.MINUTES);

    @Rule
    public TestMDC testMDC = new TestMDC();

    private final Browser browser;

    protected WebDriver driver;

    protected static Map<Browser, WebDriver> staticDrivers = new HashMap<>();

    protected static Map<Class, Boolean> loggedAboutSetupEach = new HashMap<>();
    protected boolean setupEach;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        List<Object[]> result = new ArrayList<>();
        List<String> browsers = Arrays.asList(CONFIG.getProperty("browsers").split("\\s*,\\s*"));
        if (browsers.contains("chrome")) {
            result.add(new Object[]{new Browser(DriverManagerType.CHROME, "2.41")}); // 2.41 corresponds with the chrome on jenkins.
        }
        if (browsers.contains("firefox")) {
            result.add(new Object[]{new Browser(DriverManagerType.FIREFOX, null)});
        }
        return result;
    }

    protected AbstractTest(@Nonnull Browser browser) {
        this.browser = browser;
        this.setupEach = this.getClass().getAnnotation(FixMethodOrder.class) == null;
        if (!this.setupEach && !loggedAboutSetupEach.getOrDefault(getClass(), false)) {
            log.info("\nRunning with fixed method order, so keeping the driver between the tests");
            loggedAboutSetupEach.put(getClass(), true);
        }
    }

    @Before
    public void setUp() {
        if (setupEach) {
            driver = createDriver(browser);
        } else {
            driver = staticDrivers.computeIfAbsent(browser, AbstractTest::createDriver);
        }
    }

    private static WebDriver createDriver(Browser browser) {
        try {
            WebDriver driver = browser.asWebDriver();
            // The dimension of the browser should be big enough, (headless browser seem to be small!), otherwise test will keep waiting forever
            Dimension d = new Dimension(2000, 1500);
            driver.manage().window().setSize(d);
            return driver;
        } catch (Exception e) {
            LOG.error("Could not create driver for " + browser + ":" + e.getMessage(), e);
            throw e;
        }
    }

    @After
    public void tearDown() {
        if (setupEach) {
            if (driver != null) {
                driver.close();
                driver.quit();
            }
        }
    }

    @AfterClass
    public static void tearDownClass() {
        for (WebDriver wd : staticDrivers.values()) {
            wd.quit();
        }
    }

    protected PomsLogin login(String url) {
        return new PomsLogin(url, driver);
    }

    protected PomsLogin login() {
        return login(null);
    }

    protected void logout() {
        if (driver != null) {
            Search search = new Search(driver);
            search.logout();
        } else {
            log.error("Cannot logout because no driver");
        }
    }

    protected void waitForAngularRequestsToFinish() {
        new NgWebDriver((JavascriptExecutor) driver).waitForAngularRequestsToFinish();
    }

    protected void scrollIntoView(WebElement element) {
        log.info("moving to {}", element);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        /*Actions actions = new Actions(driver);
        actions.moveToElement(element);
        actions.perform();*/
        waitForAngularRequestsToFinish();
    }
}
