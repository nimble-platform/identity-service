package eu.nimble.core.infrastructure.identity.test;

import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Random;

/**
 * Created by Johannes Innerbichler on 25/04/17.
 * Test for database connections.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class DatabaseTests {


    @Autowired
    PersonRepository personRepository;

    @Test
    public void exampleTest() {

        Random ran = new Random();
        Integer x = new Integer(ran.nextInt(6) + 5);

        PersonType person = new PersonType();
        person.setFamilyName("Innerbichler");
        person.setFirstName(x.toString());

        personRepository.save(new PersonType());
    }
}