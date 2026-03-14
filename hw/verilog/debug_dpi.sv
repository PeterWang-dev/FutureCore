module debug_dpi #(
    parameter int GPR_NUM   = 32,
    parameter int GPR_WIDTH = 32
) (
    input clk_i,
    input rst_ni,
    input [31:0] s_pc_i,
    input [31:0] s_inst_i,
    input [GprsWidth - 1:0] s_gprs_i,
    input [31:0] s_mstatus_i,
    input [31:0] s_mtvec_i,
    input [31:0] s_mepc_i,
    input [31:0] s_mcause_i
);
  localparam int GprsWidth = GPR_NUM * GPR_WIDTH;

  import "DPI-C" function void send_state(
    input bit [31:0] pc,
    input bit [31:0] inst,
    input bit [GprsWidth - 1:0] gprs,
    input bit [31:0] mstatus,
    input bit [31:0] mtvec,
    input bit [31:0] mepc,
    input bit [31:0] mcause
  );

  always_comb begin
    if (rst_ni) begin
      send_state(s_pc_i, s_inst_i, s_gprs_i, s_mstatus_i, s_mtvec_i, s_mepc_i, s_mcause_i);
    end
  end

endmodule
