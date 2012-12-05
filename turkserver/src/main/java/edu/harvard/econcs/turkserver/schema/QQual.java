package edu.harvard.econcs.turkserver.schema;

import static com.mysema.query.types.PathMetadataFactory.*;

import com.mysema.query.types.path.*;

import com.mysema.query.types.PathMetadata;
import javax.annotation.Generated;
import com.mysema.query.types.Path;


/**
 * QQual is a Querydsl query type for Qual
 */
@Generated("com.mysema.query.sql.codegen.MetaDataSerializer")
public class QQual extends com.mysema.query.sql.RelationalPathBase<Qual> {

    private static final long serialVersionUID = 2131469404;

    public static final QQual qual1 = new QQual("qual");

    public final StringPath qual = createString("qual");

    public final NumberPath<Integer> value = createNumber("value", Integer.class);

    public final StringPath workerId = createString("workerId");

    public final com.mysema.query.sql.PrimaryKey<Qual> primary = createPrimaryKey(qual, workerId);

    public final com.mysema.query.sql.ForeignKey<Worker> qual1Fk = createForeignKey(workerId, "id");

    public QQual(String variable) {
        super(Qual.class, forVariable(variable), "null", "qual");
    }

    @SuppressWarnings("all")
    public QQual(Path<? extends Qual> path) {
        super((Class)path.getType(), path.getMetadata(), "null", "qual");
    }

    public QQual(PathMetadata<?> metadata) {
        super(Qual.class, metadata, "null", "qual");
    }

}

