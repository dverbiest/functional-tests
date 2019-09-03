package nl.vpro.poms.selenium.pages;


import nl.vpro.api.client.utils.Config;
import nl.vpro.poms.selenium.util.WebDriverUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static nl.vpro.poms.selenium.poms.AbstractPomsTest.CONFIG;
import static org.assertj.core.api.Fail.fail;


/**
 * TODO: Doesn't work, should it not at least open the correct url?
 */

public class CMSMediaSelector extends AbstractPage {

    String mainCMSWindow;
    String pomsWindow;
    String UrlCmsMediaSelector = CONFIG.getProperties(Config.Prefix.poms).get("baseUrl") + "/CMSSelector/example/";
    WebDriverWait wait;


    public CMSMediaSelector(WebDriverUtil driver) {
        super(driver);
        this.wait = new WebDriverWait(webDriverUtil.getDriver(), 15, 250);
    }

    public void openUrlCmsMediaSelector() {
        driver.navigate().to(UrlCmsMediaSelector);
    }

    public void clickButtonSelect() {
        WebDriverWait wait = new WebDriverWait(driver, 15, 250);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button#select")));
        webDriverUtil.waitAndClick(By.cssSelector("button#select"));
    }

    public void switchToPomsWindows() {
        mainCMSWindow = driver.getWindowHandle();

        driver.getWindowHandles().forEach(windowHandle -> {
            driver.switchTo().window(windowHandle);
        });
        webDriverUtil.getWait().until(ExpectedConditions.titleContains("POMS"));
    }

    public void switchToCMSWindow() {
        pomsWindow = driver.getWindowHandle();
        driver.getWindowHandles().forEach(windowHandle -> {
            driver.switchTo().window(windowHandle);
        });
        wait.until(ExpectedConditions.titleContains("POMS Media selector"));
    }

    public void loginNPOGebruikerMediaSelector() {
      /*  CasLogin login = new CasLogin(webDriverUtil);
        String user = CONFIG.getProperty("SpeciaalVfGebruiker.LOGIN");
        String password = CONFIG.getProperty("SpeciaalVfGebruiker.PASSWORD");
        login.login(user, password);*/
    }

    public void checkIfNotDisplayedTables() {
        webDriverUtil.waitForInvisible(By.cssSelector("tr"));
    }

    public void checkLoginTextBoxes() {
        webDriverUtil.waitForVisible(By.id("username"));
        webDriverUtil.waitForVisible(By.id("password"));
    }

    public String getResult() {
        webDriverUtil.waitForVisible(By.cssSelector("input#value"));
        Object value = ((JavascriptExecutor) driver).executeScript("return document.querySelector('input#value').value");
        String returnValue = "";
        if (value instanceof String) {
            returnValue = value.toString();
        } else {
            fail("Error in the javascript on the page");
            System.out.println("Error in the javascript on the page");
        }
        return returnValue;
    }

}
