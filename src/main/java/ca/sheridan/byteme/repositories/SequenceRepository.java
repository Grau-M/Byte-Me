package ca.sheridan.byteme.repositories;

import ca.sheridan.byteme.beans.Sequence;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SequenceRepository extends MongoRepository<Sequence, String> {
}
