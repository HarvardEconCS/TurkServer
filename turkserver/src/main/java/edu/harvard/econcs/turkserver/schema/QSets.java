package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QSets is a Querydsl query type for Sets
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QSets extends com.mysema.query.sql.RelationalPathBase<Sets> {

    private static final long serialVersionUID = 2131514206;

    public static final QSets sets = new QSets("sets");

    public final StringPath descript = createString("descript");

    public final NumberPath<Integer> id = createNumber("id", Integer.class);

    public final StringPath name = createString("name");

    public final com.mysema.query.sql.PrimaryKey<Sets> primary = createPrimaryKey(id);

    public final com.mysema.query.sql.ForeignKey<Experiment> _experimentIbfk1 = createInvForeignKey(name, "setId");

    public final com.mysema.query.sql.ForeignKey<Session> _session2Fk = createInvForeignKey(name, "setId");

    public QSets(String variable) {
        super(Sets.class, forVariable(variable), "null", "sets");
    }

    @SuppressWarnings("all")
    public QSets(Path<? extends Sets> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "sets");
    }

    public QSets(PathMetadata<?> metadata) {
        super(Sets.class, metadata, "null", "sets");
    }

}

