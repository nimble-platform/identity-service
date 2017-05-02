package eu.nimble.core.infrastructure.identity.test;

import eu.nimble.core.infrastructure.identity.repository.PersonRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.PersonType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Random;
import java.util.stream.StreamSupport;

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
        Integer x = ran.nextInt(6) + 5;

        PersonType person = new PersonType();
        person.setFamilyName("Innerbichler");
        person.setFirstName("Johannes" + x.toString());

        PersonType addedPerson = personRepository.save(person);

        List<PersonType> allPersons = personRepository.findByHjid(addedPerson.getHjid());

        assert allPersons.stream().anyMatch(p -> p.getFamilyName().equals("Innerbichler"));
    }
}