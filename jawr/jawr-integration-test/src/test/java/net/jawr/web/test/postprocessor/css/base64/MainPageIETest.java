/**
 * 
 */
package net.jawr.web.test.postprocessor.css.base64;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlImageInput;
import com.gargoylesoftware.htmlunit.html.HtmlLink;
import com.gargoylesoftware.htmlunit.html.HtmlScript;

import net.jawr.web.test.AbstractPageTest;
import net.jawr.web.test.JawrTestConfigFiles;
import net.jawr.web.test.utils.JavaVersionUtils;
import net.jawr.web.test.utils.Utils;

/**
 * Test case for standard page in production mode.
 * 
 * @author ibrahim Chaehoi
 */
@JawrTestConfigFiles(webXml = "net/jawr/web/postprocessor/css/base64/config/web.xml", jawrConfig = "net/jawr/web/postprocessor/css/base64/config/jawr.properties")
public class MainPageIETest extends AbstractPageTest {

	/**
	 * Returns the page URL to test
	 * 
	 * @return the page URL to test
	 */
	protected String getPageUrl() {
		return getServerUrlPrefix() + getUrlPrefix() + "/index.jsp";
	}

	/**
	 * Creates the web client
	 * 
	 * @return the web client
	 */
	protected WebClient createWebClient() {

		WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER_6);
		// Defines the accepted language for the web client.
		webClient.addRequestHeader("Accept-Language", getAcceptedLanguage());
		return webClient;
	}

	@Test
	public void testPageLoad() throws Exception {

		final List<String> expectedAlerts = Collections
				.singletonList("A little message retrieved from the message bundle : Hello $ world!");
		assertEquals(expectedAlerts, collectedAlerts);
		
		if(JavaVersionUtils.isVersionInferiorToJava8()){
			assertContentEquals("/net/jawr/web/postprocessor/css/base64/resources/index-jsp-result-ie-expected.txt", page);
		}else{
			assertContentEquals("/net/jawr/web/postprocessor/css/base64/resources/index-jsp-result-ie-java8-expected.txt", page);
		}
	}

	@Test
	public void checkGeneratedJsLinks() {
		// Test generated Script link
		final List<HtmlScript> scripts = getJsScriptTags();
		assertEquals(1, scripts.size());
		final HtmlScript script = scripts.get(0);
		assertEquals(getUrlPrefix() + "/690372103.en_US/js/bundle/msg.js", script.getSrcAttribute());
	}

	@Test
	public void testJsBundleContent() throws Exception {

		final List<HtmlScript> scripts = getJsScriptTags();
		final HtmlScript script = scripts.get(0);
		final JavaScriptPage page = getJavascriptPage(script);
		assertContentEquals("/net/jawr/web/postprocessor/css/base64/resources/msg-bundle.js", page);
	}

	@Test
	public void checkGeneratedCssLinks() {
		// Test generated Css link
		final List<HtmlLink> styleSheets = getHtmlLinkTags();
		assertEquals(1, styleSheets.size());
		final HtmlLink css = styleSheets.get(0);
		if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
			assertEquals(getUrlPrefix() + "/N1749366729.ie6@/fwk/core/component.css", css.getHrefAttribute());
		} else {
			assertEquals(getUrlPrefix() + "/1109471991.ie6@/fwk/core/component.css", css.getHrefAttribute());
		}
	}

	@Test
	public void testCssBundleContent() throws Exception {

		final List<HtmlLink> styleSheets = getHtmlLinkTags();
		final HtmlLink css = styleSheets.get(0);
		final TextPage page = getCssPage(css);
		if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_1_8)) {
			assertContentEquals("/net/jawr/web/postprocessor/css/base64/resources/component-ie-java8-expected.css",
					page);
		} else {
			assertContentEquals("/net/jawr/web/postprocessor/css/base64/resources/component-ie-expected.css", page);
		}
	}

	@Test
	public void checkGeneratedHtmlImageLinks() {
		// Test generated HTML image link
		final List<?> images = getHtmlImageTags();
		assertEquals(1, images.size());
		final HtmlImage img = (HtmlImage) images.get(0);
		Utils.assertGeneratedLinkEquals(
				getUrlPrefix() + "/cbfc517da02d6a64a68e5fea9a5de472f1/img/appIcons/application.png",
				img.getSrcAttribute());

	}

	@Test
	public void checkGeneratedHtmlImageInputLinks() {
		// Test generated HTML image link
		final List<HtmlImageInput> images = getHtmlImageInputTags();
		assertEquals(1, images.size());
		final HtmlImageInput img = images.get(0);
		Utils.assertGeneratedLinkEquals(getUrlPrefix() + "/cb30a18063ef42b090194a7e936086960f/img/cog.png",
				img.getSrcAttribute());

	}

}
