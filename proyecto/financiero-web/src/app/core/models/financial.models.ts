export interface User {
    id?: number;
    name: string;
    username?: string; // <--- NUEVO
    curp: string;
    role: string;
    token?: string;
    balance?: number;
}

export interface Transaction {
    id: string;
    amount: number;
    type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER_SENT' | 'TRANSFER_RECEIVED';
    description: string;
    status: 'COMPLETED' | 'PENDING' | 'FAILED';
    date: string; // Fecha en formato texto ISO
}