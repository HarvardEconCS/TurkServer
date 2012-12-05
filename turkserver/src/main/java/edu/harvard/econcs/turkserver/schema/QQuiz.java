package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QQuiz is a Querydsl query type for Quiz
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QQuiz extends com.mysema.query.sql.RelationalPathBase<Quiz> {

    private static final long serialVersionUID = 2131469666;

    public static final QQuiz quiz = new QQuiz("quiz");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final NumberPath<Integer> numCorrect = createNumber("numCorrect", Integer.class);

    public final NumberPath<Integer> numTotal = createNumber("numTotal", Integer.class);

    public final NumberPath<Double> score = createNumber("score", Double.class);

    public final StringPath sessionId = createString("sessionId");

    public final StringPath setId = createString("setId");

    public final StringPath workerId = createString("workerId");

    public final com.mysema.query.sql.PrimaryKey<Quiz> primary = createPrimaryKey(id);

    public final com.mysema.query.sql.ForeignKey<Worker> quizIbfk2 = createForeignKey(workerId, "id");

    public QQuiz(String variable) {
        super(Quiz.class, forVariable(variable), "null", "quiz");
    }

    @SuppressWarnings("all")
    public QQuiz(Path<? extends Quiz> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "quiz");
    }

    public QQuiz(PathMetadata<?> metadata) {
        super(Quiz.class, metadata, "null", "quiz");
    }

}

