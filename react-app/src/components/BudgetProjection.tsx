import React, { useState, useEffect } from 'react';
import { Calendar, TrendingUp, TrendingDown, DollarSign, BarChart3, ChevronLeft, ChevronRight } from 'lucide-react';
import { format, addMonths, startOfMonth } from 'date-fns';
import { fr } from 'date-fns/locale';
import { apiClient } from '@/lib/api';

interface MonthlyProjection {
  month: string | Date;
  monthName: string;
  year: number;
  totalIncome: number;
  totalExpenses: number;
  netAmount: number;
  paymentDetails: PaymentDetail[];
}

interface PaymentDetail {
  paymentId: number;
  name: string;
  type: string;
  amount: number;
  dates: string[];
  frequency: string;
}

interface BudgetProjection {
  monthlyProjections: MonthlyProjection[];
  totalProjectedIncome: number;
  totalProjectedExpenses: number;
  existingBudgets?: any[];
}

interface BudgetProjectionProps {
  startDate?: Date;
  monthsAhead?: number;
  onClose?: () => void;
}

const BudgetProjection: React.FC<BudgetProjectionProps> = ({
  startDate = new Date(),
  monthsAhead = 12,
  onClose
}) => {
  const [projection, setProjection] = useState<BudgetProjection | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedMonth, setSelectedMonth] = useState<number>(0);

  useEffect(() => {
    // UN SEUL appel à l'ouverture du composant
    fetchProjection();
  }, []); // Pas de dependencies - on ne veut qu'un seul appel!

  const fetchProjection = async () => {
    try {
      setLoading(true);
      setError(null);

      const formattedDate = startOfMonth(startDate).toISOString().split('T')[0];
      const data = await apiClient.getBudgetMultiMonthProjection(formattedDate, monthsAhead);
      
      if (data && typeof data === 'object') {
        setProjection(data);
      } else {
        setError('Données de projection invalides');
      }
    } catch (error: any) {
      console.error('Error fetching budget projection:', error);
      
      if (error.response?.status === 404) {
        setError('L\'API de projection budgétaire n\'est pas encore implémentée côté backend.');
      } else if (error.code === 'ERR_NETWORK') {
        setError('Impossible de se connecter au serveur. Vérifiez que le backend est démarré.');
      } else {
        setError('Une erreur est survenue lors du chargement de la projection.');
      }
      setProjection(null);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  };

  const getAmountColor = (amount: number) => {
    if (amount > 0) return 'text-green-600';
    if (amount < 0) return 'text-red-600';
    return 'text-gray-600';
  };

  const getAmountIcon = (amount: number) => {
    if (amount > 0) return <TrendingUp className="h-4 w-4" />;
    if (amount < 0) return <TrendingDown className="h-4 w-4" />;
    return <DollarSign className="h-4 w-4" />;
  };

  const navigateMonth = (direction: 'prev' | 'next') => {
    if (!projection || !projection.monthlyProjections || projection.monthlyProjections.length === 0) return;
    
    if (direction === 'prev' && selectedMonth > 0) {
      setSelectedMonth(selectedMonth - 1);
    } else if (direction === 'next' && selectedMonth < projection.monthlyProjections.length - 1) {
      setSelectedMonth(selectedMonth + 1);
    }
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-6">
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-6">
        <div className="text-center text-red-600">
          <p className="text-lg font-medium">Erreur</p>
          <p className="text-sm mt-2">{error}</p>
          <button
            onClick={fetchProjection}
            className="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  if (!projection || !projection.monthlyProjections || !Array.isArray(projection.monthlyProjections) || projection.monthlyProjections.length === 0) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-6">
        <div className="text-center text-gray-600">
          <Calendar className="h-12 w-12 mx-auto mb-4 text-gray-400" />
          <p className="text-lg font-medium">Aucune projection disponible</p>
          <p className="text-sm mt-2">
            Ajoutez des paiements récurrents pour voir vos projections budgétaires.
          </p>
          {onClose && (
            <button
              onClick={onClose}
              className="mt-4 bg-gray-600 text-white px-4 py-2 rounded-lg hover:bg-gray-700"
            >
              Fermer
            </button>
          )}
        </div>
      </div>
    );
  }

  // Vérifier que selectedMonth est valide
  const safeSelectedMonth = Math.max(0, Math.min(selectedMonth, projection.monthlyProjections.length - 1));
  const currentMonthData = projection.monthlyProjections[safeSelectedMonth];

  return (
    <div className="bg-white rounded-lg shadow-lg">
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <BarChart3 className="h-6 w-6 text-blue-600" />
            <h2 className="text-xl font-semibold text-gray-900">
              Projection budgétaire multi-mois
            </h2>
          </div>
          {onClose && (
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {/* Summary Statistics */}
      <div className="px-6 py-4 bg-gray-50 border-b">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="text-center">
            <p className="text-sm text-gray-600">Total des revenus projetés</p>
            <p className="text-2xl font-semibold text-green-600">
              {formatCurrency(projection.totalProjectedIncome)}
            </p>
          </div>
          <div className="text-center">
            <p className="text-sm text-gray-600">Total des dépenses projetées</p>
            <p className="text-2xl font-semibold text-red-600">
              {formatCurrency(projection.totalProjectedExpenses)}
            </p>
          </div>
          <div className="text-center">
            <p className="text-sm text-gray-600">Solde net projeté</p>
            <p className={`text-2xl font-semibold ${getAmountColor(projection.totalProjectedIncome - projection.totalProjectedExpenses)}`}>
              {formatCurrency(projection.totalProjectedIncome - projection.totalProjectedExpenses)}
            </p>
          </div>
        </div>
      </div>

      {/* Month Navigation */}
      <div className="px-6 py-4 border-b">
        <div className="flex items-center justify-between">
          <button
            onClick={() => navigateMonth('prev')}
            disabled={selectedMonth === 0}
            className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <ChevronLeft className="h-4 w-4" />
            <span>Précédent</span>
          </button>

          <h3 className="text-lg font-semibold text-gray-900">
            {format(new Date(currentMonthData.year, new Date(currentMonthData.month).getMonth()), 'MMMM yyyy', { locale: fr })}
          </h3>

          <button
            onClick={() => navigateMonth('next')}
            disabled={selectedMonth === projection.monthlyProjections.length - 1}
            className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-800 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span>Suivant</span>
            <ChevronRight className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Monthly Details */}
      <div className="px-6 py-4">
        {/* Monthly Summary */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-green-50 p-4 rounded-lg border border-green-200">
            <div className="flex items-center space-x-2">
              <TrendingUp className="h-5 w-5 text-green-600" />
              <span className="text-sm font-medium text-green-800">Revenus</span>
            </div>
            <p className="text-xl font-semibold text-green-600 mt-2">
              {formatCurrency(currentMonthData.totalIncome)}
            </p>
          </div>

          <div className="bg-red-50 p-4 rounded-lg border border-red-200">
            <div className="flex items-center space-x-2">
              <TrendingDown className="h-5 w-5 text-red-600" />
              <span className="text-sm font-medium text-red-800">Dépenses</span>
            </div>
            <p className="text-xl font-semibold text-red-600 mt-2">
              {formatCurrency(currentMonthData.totalExpenses)}
            </p>
          </div>

          <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
            <div className="flex items-center space-x-2">
              {getAmountIcon(currentMonthData.netAmount)}
              <span className="text-sm font-medium text-blue-800">Solde net</span>
            </div>
            <p className={`text-xl font-semibold mt-2 ${getAmountColor(currentMonthData.netAmount)}`}>
              {formatCurrency(currentMonthData.netAmount)}
            </p>
          </div>
        </div>

        {/* Payment Details */}
        {currentMonthData.paymentDetails.length > 0 && (
          <div>
            <h4 className="text-lg font-medium text-gray-900 mb-4">
              Détail des paiements prévus
            </h4>
            <div className="space-y-3">
              {currentMonthData.paymentDetails.map((payment, index) => (
                <div
                  key={`${payment.paymentId}-${index}`}
                  className="flex items-center justify-between p-3 bg-gray-50 rounded-lg"
                >
                  <div className="flex-1">
                    <p className="font-medium text-gray-900">{payment.name}</p>
                    <div className="flex items-center space-x-4 text-sm text-gray-600">
                      <span className="capitalize">{payment.frequency.toLowerCase()}</span>
                      <span>
                        {payment.dates.length} paiement{payment.dates.length > 1 ? 's' : ''}
                      </span>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                        payment.type === 'INCOME' 
                          ? 'bg-green-100 text-green-800' 
                          : payment.type === 'CREDIT'
                          ? 'bg-orange-100 text-orange-800'
                          : 'bg-red-100 text-red-800'
                      }`}>
                        {payment.type === 'INCOME' ? 'Revenu' : payment.type === 'CREDIT' ? 'Crédit' : 'Dépense'}
                      </span>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className={`font-semibold ${
                      payment.type === 'INCOME' ? 'text-green-600' : 'text-red-600'
                    }`}>
                      {payment.type === 'INCOME' ? '+' : '-'}{formatCurrency(Math.abs(payment.amount))}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Month Timeline */}
      <div className="px-6 py-4 border-t bg-gray-50">
        <h4 className="text-sm font-medium text-gray-700 mb-3">Évolution sur {monthsAhead} mois</h4>
        <div className="flex space-x-2 overflow-x-auto pb-2">
          {projection.monthlyProjections.map((month, index) => (
            <button
              key={index}
              onClick={() => setSelectedMonth(index)}
              className={`flex-shrink-0 p-3 rounded-lg text-center transition-colors ${
                index === selectedMonth
                  ? 'bg-blue-100 border-2 border-blue-500'
                  : 'bg-white border border-gray-200 hover:bg-gray-50'
              }`}
            >
              <p className="text-xs font-medium text-gray-600">
                {format(new Date(month.year, new Date(month.month).getMonth()), 'MMM', { locale: fr })}
              </p>
              <p className={`text-sm font-semibold mt-1 ${getAmountColor(month.netAmount)}`}>
                {formatCurrency(month.netAmount)}
              </p>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default BudgetProjection;