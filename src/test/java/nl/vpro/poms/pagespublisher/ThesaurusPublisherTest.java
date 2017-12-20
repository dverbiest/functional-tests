package nl.vpro.poms.pagespublisher;

import lombok.extern.slf4j.Slf4j;

import org.junit.*;
import org.junit.runners.MethodSorters;

import nl.vpro.api.client.resteasy.PageUpdateApiClient;
import nl.vpro.api.client.utils.Config;
import nl.vpro.poms.AbstractApiTest;
import nl.vpro.poms.DoAfterException;
import nl.vpro.rs.thesaurus.update.NewPerson;
import nl.vpro.rs.thesaurus.update.NewPersonRequest;

import static org.junit.Assume.assumeNoException;

/**
 * @author Michiel Meeuwissen
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
@Ignore("Not yet finished. Issue is still in progress")
public class ThesaurusPublisherTest extends AbstractApiTest {

   PageUpdateApiClient pageUpdateApiClient = PageUpdateApiClient.configured(
       CONFIG.env(),
       CONFIG.getProperties(Config.Prefix.pageupdate_api)
   ).build();


    @Rule
    public DoAfterException doAfterException = new DoAfterException((t) -> {
        ThesaurusPublisherTest.exception = t;
    });

    private static Throwable exception = null;

    @Before
    public void setup() {
        assumeNoException(exception);
    }


    @Test
    public void test001CreatePerson() {
        pageUpdateApiClient.getThesaurusUpdateRestService().submitSigned(
            NewPersonRequest.builder().person(
                NewPerson.builder()
                    .familyName("Puk")
                    .givenName("Pietje2" + System.currentTimeMillis())
                    .build()
            ).build());

    }

    @Test
    public void test100Arrived() {
        pageUpdateApiClient.getThesaurusUpdateRestService().submitSigned(
            NewPersonRequest.builder().person(
                NewPerson.builder()
                    .familyName("Puk")
                    .givenName("Pietje2").build()
            ).build());
    }

}
