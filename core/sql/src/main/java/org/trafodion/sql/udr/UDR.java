// @@@ START COPYRIGHT @@@
//
// (C) Copyright 2015 Hewlett-Packard Development Company, L.P.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
// @@@ END COPYRIGHT @@@

package org.trafodion.sql.udr;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;

/**
 * This class represents the code associated with a UDR.
 * <p>
 * UDR writers can create a derived class and implement these methods
 * for their specific UDR. The base class also has default methods
 * for all but the runtime call {@link UDR#processData(UDRInvocationInfo, UDRPlanInfo)}. See 
 * https://wiki.trafodion.org/wiki/index.php/Tutorial:_The_object-oriented_UDF_interface
 * for examples.
 * <p>
 * A UDR writer can decide to override none, some or all of the virtual         
 * methods that comprise the complier interaction. The run-time interaction
 * {@link UDR#processData(UDRInvocationInfo, UDRPlanInfo)}, must always be provided.
 * <p>
 * When overriding methods, the UDR writer has the option to call the default
 * method to do part of the work, and then to implement additional logic.
 * <p>
 * Multiple UDRs could share the same subclass of UDR. The UDR name is passed
 * in UDRInvocationInfo, so the logic can depend on the name.
 * <p>
 * A single query may invoke the same UDR more than once. A different
 * UDRInvocationInfo object will be passed for each such invocation.
 * <p>
 * The UDR object or the object of its derived class may be reused for
 * multiple queries, so its life time can exceed that of a UDRInvocation
 * object.
 * <p>
 * Different instances of UDR (or derived class)objects will be created
 * in the processes that compile and execute a query.
 * <p>
 * Based on the previous three bullets, UDR writers should not store state
 * that relates to a UDR invocation in a UDR (or derived) object. There are
 * special classes to do that. It is ok to use the UDR derived class to store
 * resources that are shared between UDR invocations, such as connections to
 * server processes etc. These need to be cleaned up in {@link UDR#close()}.
 * <p>
 * The optimizer may try different execution plans for a UDR invocation, e.g.
 * with different partitioning and ordering of input and/or output data. These
 * alternative plans share the same UDRInvocationInfo object but they will use
 * different UDRPlanInfo objects.
 */
public abstract class UDR
{
    
    public UDR()
    {
    };
    
    public void close()
    {
    };

    // compile time interface for UDRs

    /**
     * First method called during compilation of a TMUDF invocation.
     * <p>
     *  Describe the output columns of a TMUDF, based on a description of
     *  its parameters (including parameter values that are specified as a
     *  constant) and the description of the table-valued input columns.
     * <p>
     *  When the compiler calls this, it will have set up the formal and
     *  actual parameter descriptions as well as an output column
     *  description containing all the output parameters defined in the
     *  CREATE FUNCTION DDL (if any).
     * <p>
     *  This method should do a general check of things it expects that can be
     *  validated at this time such as input table columns. It should then generate
     *  a description of the table-valued output columns, if applicable
     *  and if the columns provided at DDL time are not sufficient. The
     *  "See also" section points to methods to set these values.
     * <p>
     *  Columns of the table-valued output can be declard as "pass-thru"
     *  columns to make many optimizations simpler.
     * <p>
     *  This method must also add or alter the formal parameter list
     *  to match the list of actual parameters.
     * <p>
     *  The default implementation does nothing. If this method is not used, all
     *  parameters and result table columns must be declared in the
     *  CREATE TABLE MAPPING FUNCTION DDL.
     
     *  @see UDRInvocationInfo#addFormalParameter(ColumnInfo)
     *  @see UDRInvocationInfo#setFuncType(FuncType)
     *  @see UDRInvocationInfo#addPassThruColumns()
     *  @see TupleInfo#addColumn(ColumnInfo)
     *  @see TupleInfo#addIntColumn(String, boolean)
     *  @see TupleInfo#addLongColumn(String, boolean)
     *  @see TupleInfo#addCharColumn(String, int, boolean, TypeInfo.SQLCharsetCode, TypeInfo.SQLCollationCode)
     *  @see TupleInfo#addVarCharColumn(String, int, boolean, TypeInfo.SQLCharsetCode, TypeInfo.SQLCollationCode)
     *  @see TupleInfo#addColumns(Vector)
     *  @see TupleInfo#addColumnAt(ColumnInfo, int)
     *  @see TupleInfo#deleteColumn(int)
     *  @see TupleInfo#deleteColumn(String)
     *
     *  @param info A description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describeParamsAndColumns(UDRInvocationInfo info)
        throws UDRException
    {
        
    };


    /**
     * Eliminate unneeded columns and decide where to execute predicates.
     * <p>
     * This is the second call in the compiler interaction, after
     * describeParamsAndColumns(). When the compiler calls this, it will have
     * marked the UDF result columns with a usage code, indicating any output
     * columns that are not required for this particular query. It will also have
     * created a list of predicates that need to be evaluated.
     * <p>
     * This method can mark any of the columns of the table-valued inputs as not
     * used, based on the result column usage and internal needs of the UDF. It can
     * also decide where to evaluate each predicate, a) on the UDF result,
     * b) inside the UDF and c) in the table-valued inputs.
     * <p>
     * The default implementation does not mark any of the table-valued input
     * columns as unused. Predicate handling in the default implementation
     * depends on the function type:
     * <ul>
     * <li> 
     * GENERIC: No predicates are pushed down, because the compiler does not
     *          know whether any of the eliminated rows might have altered the
     *          output of the UDF. One example is the "sessionize" UDF, where
     *          eliminated rows can lead to differences in session ids.
     * </li>
     * <li>
     * MAPPER:  All predicates on pass-thru columns are pushed down to table-valued
     *          inputs. Since the UDF carries no state between the input rows it
     *          sees, eliminating any input rows will not alter results for other
     *          rows.
     * </li>
     * <li>
     * REDUCER: Only predicates on the PARTITION BY columns will be pushed to
     *          table-valued inputs. These predicates may eliminate entire groups
     *          of rows (partitions), and since no state is carried between such
     *          groups that is valid.
     * </li>
     * </ul>
     * <p>
     *  @see ColumnInfo#getUsage()
     *  @see UDRInvocationInfo#setFuncType(FuncType)
     *  @see UDRInvocationInfo#setChildColumnUsage(int, int, ColumnInfo.ColumnUseCode)
     *  @see UDRInvocationInfo#setUnusedPassthruColumns()
     *  @see UDRInvocationInfo#pushPredicatesOnPassthruColumns()
     *  @see UDRInvocationInfo#setPredicateEvaluationCode(int, PredicateInfo.EvaluationCode)
     *
     *  @param info A description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describeDataflowAndPredicates(UDRInvocationInfo info)  
        throws UDRException
    {
        switch (info.getFuncType())
        {
        case GENERIC:
            break;

        case MAPPER:
            // push as many predicates as possible to the children
            info.pushPredicatesOnPassthruColumns();
            break;

        case REDUCER:
        {
            int partitionedChild = -1;

            // find a child that uses a PARTITION BY
            for (int c=0; c<info.getNumTableInputs(); c++)
                if (info.in(c).getQueryPartitioning().getType() ==
                    PartitionInfo.PartitionTypeCode.PARTITION)
                    partitionedChild = c;

            if (partitionedChild >= 0)
            {
                final PartitionInfo partInfo =
                    info.in(partitionedChild).getQueryPartitioning();
                int numPredicates = info.getNumPredicates();

                // walk through all comparison predicates
                for (int p=0; p<numPredicates; p++)
                    if (info.isAComparisonPredicate(p))
                    {
                        // a predicate on column "predCol"
                        int predCol = 
                            info.getComparisonPredicate(p).getColumnNumber();

                        // check whether predCol appears in the PARTITION BY clause
                        Iterator<Integer> it = partInfo.getPartCols().iterator();
                        while (it.hasNext())
                            if (predCol == it.next().intValue())
                                // yes, this is a predicate on a partitioning column,
                                // push it down if possible
                                info.pushPredicatesOnPassthruColumns(p,-1);
                    }
            }
        }
            break;

        default:
            throw new UDRException(
                                   38900,
                                   "Invalid UDR Function type: %s",
                                   info.getFuncType().name());
        }
    };

    /**
     *  
     *  @param info A description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describeConstraints(UDRInvocationInfo info)
        throws UDRException
    {
    };

    /**
     *  
     *  @param info A description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describeStatistics(UDRInvocationInfo info)
        throws UDRException
    {
    };
    
    /**
     *  Describe the desired parallelism of a UDR.
     *  <p>
     *  This method can be used to specify a desired degree of
     *  parallelism, either in absolute or relative terms.
     *  <p>
     *  The default behavior is to allow any degree of parallelism for
     *  TMUDFs with one table-valued input, and to force serial execution
     *  in all other cases. The reason is that for a single table-valued
     *  input, there is a natural way to parallelize the function by
     *  parallelizing its input a la MapReduce. In all other cases,
     *  parallel execution requires active participation by the UDF,
     *  which is why the UDF needs to signal explicitly that it can
     *  handle such flavors of parallelism.
     * <p>
     *  Note that this is NOT foolproof, and that the TMUDF might still
     *  need to validate the PARTITION BY and ORDER BY syntax used in its
     *  invocation.
     * <p>
     *  @see UDRPlanInfo#getDesiredDegreeOfParallelism
     *  @see UDRPlanInfo#setDesiredDegreeOfParallelism
     *  @see UDRInvocationInfo#getNumParallelInstances
     *
     *  @param info A description of the UDR invocation.
     *  @param plan Plan-related description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describeDesiredDegreeOfParallelism(UDRInvocationInfo info,
                                                   UDRPlanInfo plan)
         throws UDRException
    {
        if (info.getNumTableInputs() == 1)
            plan.setDesiredDegreeOfParallelism(UDRPlanInfo.SpecialDegreeOfParallelism.ANY_DEGREE_OF_PARALLELISM.SpecialDegreeOfParallelism());
        else
            plan.setDesiredDegreeOfParallelism(1); // serial execution
    };

    /**
     *  @param info A description of the UDR invocation.
     *  @param plan Plan-related description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void describePlanProperties(UDRInvocationInfo info,
                                       UDRPlanInfo plan)
        throws UDRException 
    { 
    };
    
    /**
     *  Final call of the compiler interaction for TMUDFs.
     *  <p>
     *  This final compile time call gives the UDF writer the opportunity
     *  to examine the chosen query plan, to pass information on to the
     *  runtime method, using {@link UDRPlanInfo#addPlanData(byte[]) addPlanData}, and to
     *  clean up any resources related to the compile phase of a particular TMUDF
     *  invocation.
     *  <p>
     *  The default implementation does nothing.
     *  <p>
     *  @see UDRPlanInfo#addPlanData(byte[])
     *  @see UDRPlanInfo#getUDRWriterCompileTimeData()
     *  @see UDRInvocationInfo#getUDRWriterCompileTimeData()
     * 
     *  @param info A description of the UDR invocation.
     *  @param plan Plan-related description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public void completeDescription(UDRInvocationInfo info,
                                    UDRPlanInfo plan)
        throws UDRException 
    { 
    };

    // run time interface for TMUDFs and scalar UDFs (once supported)

    /**
     *  Runtime code for UDRs.
     * <p>
     * This method must be implemented in derived class.
     * 
     *  @param info A description of the UDR invocation.
     *  @param plan Plan-related description of the UDR invocation.
     *  @throws UDRException If an exception occured in the UDR
     */
    public abstract void processData(UDRInvocationInfo info,
                                     UDRPlanInfo plan)
         throws UDRException;

    // methods to be called from the run time interface for UDRs:

    // read a row from an input table

    /**
     *  Read a row of a table-value input.
     *  <p>
     *  This method can only be called from within processData().
     *  
     *  @param info A description of the UDR invocation.
     *  @return true if another row could be read, false if it reached end of data.
     *  @throws UDRException If an exception occured in the UDR
     */
    public final boolean getNextRow(UDRInvocationInfo info) throws UDRException
    { return true; }

    // produce a result row
    
    /**
     *  Emit a row of a table-valued result.
     *  <p>
     *  This method can only be called from within UDR#processData(UDRInvocationInfo, UDRPlanInfo).
     *  
     *  @param info A description of the UDR invocation.
     * 
     *  @throws UDRException If an exception occured in the UDR
     */
    public final void emitRow(UDRInvocationInfo info) throws UDRException
    { 
        String row = info.out().getRow().asCharBuffer().toString();
        System.out.println(row);
    }

    // methods for debugging
    /**
     *  Debugging hook for UDRs.
     *
     *  This method is called in debug Trafodion builds when certain
     *  flags are set in the UDR_DEBUG_FLAGS CQD (CONTROL QUERY DEFAULT).
     *  See https://wiki.trafodion.org/wiki/index.php/Tutorial:_The_object-oriented_UDF_interface#Debugging_UDF_code
     *  for details.
     *
     *  The default implementation prints out the process id and then
     *  goes into an endless loop. The UDF writer can then attach a
     *  debugger, set breakpoints and force the execution out of the loop.
     *
     *  Note that the printout of the pid may not always be displayed on
     *  a terminal, for example if the process is executing on a different node.
     */
    public final void debugLoop()
    {
        int debugLoop = 1;
        int myPid = 0;
        try {myPid = Integer.parseInt(new File("/proc/self").getCanonicalFile().getName());}
        catch (IOException e1) {}
        
        System.out.print(String.format("Process %d entered a loop to be able to debug it\n", myPid));
        
        // go into a loop to allow the user to attach a debugger,
        // if requested, set debugLoop = 2 in the debugger to get out
        while (debugLoop < 2)
            debugLoop = 1-debugLoop;
        
    };

    // methods for versioning of this interface
    public final int getCurrentVersion() { return 1; }

    /**
     *  For versioning, return features supported by the UDR writer.
     * <p>
     *  This method can be used in the future to facilitate changes in
     *  the UDR interface. UDR writers will be able to indicte through this
     *  method whether they support new features.
     * <p>
     *  The default implementation returns 0 (no extra features are supported).
     * <p>
     *  @return A yet to be determined set of bit flags or codes for supported features.
     */
    public int getFeaturesSupportedByUDF(){ return 0; }
    
}