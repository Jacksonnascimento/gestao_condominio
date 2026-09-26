import type { NextConfig } from 'next';

// Endereço da API visto pelo servidor do Next (no Docker, o nome do serviço).
const apiInterna = process.env.API_INTERNA_URL || 'http://localhost:8080';

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      // O navegador chama o mesmo endereço da tela; o Next repassa à API com o X-Forwarded-Host original,
      // e a API escolhe o condomínio (e o banco) por ele.
      { source: '/api/v1/:path*', destination: `${apiInterna}/api/v1/:path*` },
    ];
  },
};

export default nextConfig;
