package edu.fsuj.csb.reactionnetworks.interaction;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.UnsupportedRepresentationException;
import org.jgap.impl.BooleanGene;


public class SubstanceGene extends BooleanGene implements Gene{

  private static final long serialVersionUID = -4408764795321340270L;
	private Integer substanceId;
	
	public SubstanceGene(Configuration a_config,Integer substanceId) throws InvalidConfigurationException {
	  super(a_config);
	  this.substanceId=substanceId;
  }

  public String toString() {
    return getPersistentRepresentation();
  }


  public String getPersistentRepresentation() {
  	return this.getClass().getSimpleName()+"("+substanceId+")["+getInternalValue()+"]";    
  }

	@Override
  public void setValueFromPersistentRepresentation(String a_representation) throws UnsupportedOperationException, UnsupportedRepresentationException {
		if (!a_representation.startsWith(this.getClass().getSimpleName()+"(")) throw new UnsupportedRepresentationException(a_representation);
		int start=a_representation.indexOf("(");
		if (start<0) throw new UnsupportedRepresentationException(a_representation);
		int end=a_representation.indexOf(")");
		if (end<0) throw new UnsupportedRepresentationException(a_representation);		
		substanceId=Integer.parseInt(a_representation.substring(start+1,end-1));
		start=a_representation.indexOf("[");
		if (start<0) throw new UnsupportedRepresentationException(a_representation);
		end=a_representation.indexOf("]");
		if (end<0) throw new UnsupportedRepresentationException(a_representation);		
		String dummy=a_representation.substring(start+1,end-1).toLowerCase();
		if (dummy.equals("true")) {
			super.setAllele(Boolean.TRUE);
		} else if (dummy.equals("false")){
			super.setAllele(Boolean.FALSE);
		} else throw new UnsupportedRepresentationException(dummy);
		System.out.println(this);
  }
	
  protected Gene newGeneInternal() {
    try {
      return new SubstanceGene(getConfiguration(),substanceId);
    }
    catch (InvalidConfigurationException iex) {
      throw new IllegalStateException(iex.getMessage());
    }
  }

	public Integer substanceId() {
	  return substanceId;
  }
}
